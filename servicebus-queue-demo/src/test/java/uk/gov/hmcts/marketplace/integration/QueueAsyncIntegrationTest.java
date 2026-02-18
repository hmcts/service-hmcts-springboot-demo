package uk.gov.hmcts.marketplace.integration;

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.awaitility.Awaitility.await;

@Slf4j
public class QueueAsyncIntegrationTest extends QueueIntegrationTestBase {

    @BeforeEach
    void beforeEach() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
        purgeMessages(queueName);
    }

    @SneakyThrows
    @Test
    void try_stuff() {
        ServiceBusSenderClient serviceBusSenderClient = configService
                .clientBuilder()
                .sender()
                .queueName(queueName)
                .buildClient();
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(message);
        serviceBusMessage.setScheduledEnqueueTime(OffsetDateTime.now().plusSeconds(1));
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();


        AmqpRetryOptions retryOptions = new AmqpRetryOptions();
        retryOptions.setTryTimeout(Duration.ofSeconds(60));
        retryOptions.setMaxRetries(3);
        retryOptions.setDelay(Duration.ofMillis(800));
        ServiceBusReceiverAsyncClient receiver = configService.clientBuilder()
                .retryOptions(retryOptions)
                .receiver()
                .queueName(queueName)
                .buildAsyncClient();

        System.out.println("PEEKING");
        receiver.peekMessage().subscribe(
                message -> {
                    System.out.println("Received Message Id: " + message.getMessageId());
                    System.out.println("Received Message: " + message.getBody().toString());
                },
                error -> {
                    System.err.println("Error occurred while receiving message: " + error);
                },
                () -> {
                    System.out.println("Receiving complete.");
                });
        System.out.println("DONE");
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
}
