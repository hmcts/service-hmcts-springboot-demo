package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.administration.models.CreateQueueOptions;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import com.azure.messaging.servicebus.administration.models.QueueRuntimeProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
public class QueueAdminIntegrationTest extends QueueIntegrationTestBase {

    @BeforeEach
    void beforeEach() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
        purgeMessages(queueName);
    }

    @Test
    void admin_console_should_get_queues() {
        List<String> queues = adminClient.listQueues().stream().map(QueueProperties::getName).toList();
        assertThat(queues).isEqualTo(List.of("queue.1"));

        QueueRuntimeProperties queueProperties = adminClient.getQueueRuntimeProperties(queueName);
        log.info("activeMessageCount:{} deadLetterCount:{}", queueProperties.getActiveMessageCount(), queueProperties.getDeadLetterMessageCount());
    }

    @Test
    void admin_console_should_create_new_queue() {
        CreateQueueOptions createQueueOptions = new CreateQueueOptions();
        createQueueOptions.setDeadLetteringOnMessageExpiration(true);
        createQueueOptions.setDefaultMessageTimeToLive(Duration.ofHours(1));
        adminClient.createQueue("queue.new", createQueueOptions);

        adminClient.deleteQueue("queue.new");
    }

    @SneakyThrows
    @Test
    void create_queue_should_allow_set_retry_time() {
        ServiceBusSenderClient serviceBusSenderClient = configService
                .clientBuilder()
                .sender()
                .queueName(queueName)
                .buildClient();
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage("0," + message);
        serviceBusMessage.setScheduledEnqueueTime(OffsetDateTime.now().plusSeconds(1));
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();

        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder builder = configService
                .clientBuilder()
                .processor()
                .queueName(queueName)
                .processMessage(context -> handleMessage(queueName, context))
                .processError(context -> handleError(queueName, context));
        processFor(builder.buildProcessorClient(), 2000);

        Thread.sleep(60000);
    }

    private void handleMessage(String queueName, ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        try {
            throw new RuntimeException("process failed");
        } catch (Exception e) {
            // We don't surface the error
            // Instead we requeue the message to process and let this one drop off
            String messageBody = String.valueOf(message.getBody());
            int retryCount = Integer.valueOf(messageBody.replaceAll(",.*$", ""));
            ServiceBusMessage serviceBusMessage = new ServiceBusMessage(messageBody.replaceAll(".*,", String.valueOf(retryCount + 1) + ","));
            serviceBusMessage.setScheduledEnqueueTime(retryTime(retryCount));
            ServiceBusSenderClient serviceBusSenderClient2 = configService
                    .clientBuilder()
                    .sender()
                    .queueName(queueName)
                    .buildClient();
            serviceBusSenderClient2.sendMessage(serviceBusMessage);
            serviceBusSenderClient2.close();
        }
    }

    private void handleError(String queueName, ServiceBusErrorContext context) {
        // WARNING - This method is not 100% atomically secure if the service crashes in the gap we may end up with 2 messages
        // But fine for low throughput transactions
        retryTime(1);
        log.error("error processing subscription message - {}", context.getException().getMessage());
    }

    private void throwError(String queueName, ServiceBusReceivedMessageContext context) {
        throw new RuntimeException("An error to force retry");
    }

    private OffsetDateTime retryTime(int failureCount) {
        int oneMinute = 60;
        int oneHour = 60 * 60;
        int[] retryConfig = {1, 5, 10, oneMinute, oneMinute, oneMinute, oneHour};
        int retryIndex = failureCount < retryConfig.length ? failureCount : retryConfig.length - 1;
        int retryDelaySecs = retryConfig[retryIndex];
        log.info("retryTime in {} seconds", retryDelaySecs);
        return OffsetDateTime.now().plusSeconds(retryDelaySecs);
    }

    @SneakyThrows
    private void processFor(ServiceBusProcessorClient processorClient, int processingMilliSecs) {
        log.info("starting processor");
        processorClient.start();
//        TimeUnit.MILLISECONDS.sleep(processingMilliSecs);
//        log.info("stopping processor");
//        processorClient.close();
    }
}
