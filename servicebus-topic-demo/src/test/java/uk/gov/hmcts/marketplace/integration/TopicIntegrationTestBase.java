package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.models.SubQueue;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;
import uk.gov.hmcts.marketplace.service.TopicService;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class TopicIntegrationTestBase {

    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    TopicService topicService;

    private static int messageCount;

    String topicName = "topic.1";
    String subscription1 = "subscription.1";
    String subscription2 = "subscription.2";
    int maxDeliveryCount = 3;
    String message = String.format("My message %04d", new Random().nextInt(100));

    protected boolean isServiceBusReady() {
        try {
            topicService.processMessages(topicName, subscription1, 100);
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }

    @SneakyThrows
    public void purgeMessages(String topicName, String subscriptionName) {
        log.info("purgeMessages {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = topicService.processorClientBuilder(topicName, subscriptionName, false, 100);
        processorBuilder.processMessage(context -> purgeMessage(topicName, subscriptionName, context));
        processorBuilder.processError(context -> topicService.handleError(topicName, subscriptionName, context));

        ServiceBusProcessorClient processor = processorBuilder.buildProcessorClient();
        processor.start();
        TimeUnit.MILLISECONDS.sleep(100);
        processor.stop();

        log.info("purgeMessages DLQ {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorDlqBuilder = topicService.processorClientBuilder(topicName, subscriptionName, false, 100);
        processorDlqBuilder.processMessage(context -> purgeMessage(topicName, subscriptionName, context));
        processorDlqBuilder.processError(context -> topicService.handleError(topicName, subscriptionName, context));

        ServiceBusProcessorClient processorDlq = processorDlqBuilder.buildProcessorClient();
        processorDlq.start();
        TimeUnit.MILLISECONDS.sleep(100);
        processorDlq.stop();
    }

    // TEST ONLY - Not thread safe
    @SneakyThrows
    public int countMessages(String topicName, String subscriptionName) {
        messageCount = 0;
        ServiceBusReceiverAsyncClient receiver = new ServiceBusClientBuilder()
                .connectionString(configService.getConnectionString())
                .receiver()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .buildAsyncClient();

        receiver.peekMessage().subscribe(
                message -> {
                    System.out.println("Count Message Id: " + message.getMessageId());
                    messageCount++;
                },
                error -> System.err.println("Error occurred while Counting message: " + error),
                () -> {
                    System.out.println("Count complete.");
                });
        TimeUnit.MILLISECONDS.sleep(100);

        receiver.close();
        return messageCount;
    }

    // TEST ONLY - Not thread safe
    @SneakyThrows
    public int countDeadLetterMessages(String topicName, String subscriptionName) {
        messageCount = 0;
        ServiceBusReceiverAsyncClient receiver = new ServiceBusClientBuilder()
                .connectionString(configService.getConnectionString())
                .receiver()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .buildAsyncClient();

        receiver.peekMessage().subscribe(
                message -> {
                    System.out.println("Count Message Id: " + message.getMessageId());
                    messageCount++;
                },
                error -> System.err.println("Error occurred while Counting message: " + error),
                () -> {
                    System.out.println("Count complete.");
                });
        TimeUnit.MILLISECONDS.sleep(100);
        receiver.close();
        return messageCount;
    }

    @SneakyThrows
    // TEST ONLY - Not thread safe
    // Actually does not work cos of peek abanadon not moving into the next message
    private int countMessages(String topicName, String subscriptionName, boolean dlq) {
        messageCount = 0;
        log.info("countMessages {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = topicService.processorClientBuilder(topicName, subscriptionName, false, 100);
        processorBuilder.receiveMode(ServiceBusReceiveMode.PEEK_LOCK);
        processorBuilder.processMessage(context -> countMessage(topicName, subscriptionName, context));
        if (dlq) {
            processorBuilder.subQueue(SubQueue.DEAD_LETTER_QUEUE);
        }
        ServiceBusProcessorClient processor = processorBuilder.buildProcessorClient();
        processor.start();
        TimeUnit.MILLISECONDS.sleep(1000);
        processor.stop();
        System.out.println("returning messageCount:" + messageCount);
        return messageCount;
    }

    private void purgeMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        log.info("purgeMessage {}/{} {}", topicName, subscriptionName, context.getMessage().getMessageId());
        // do nothing just let the message complete
    }

    private void countMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        log.info("countMessage {}/{} {}", topicName, subscriptionName, context.getMessage().getMessageId());
        messageCount++;
        context.abandon();
        System.out.println("incrementing messageCount to:" + messageCount);
    }
}
