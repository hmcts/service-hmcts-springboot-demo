package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.models.SubQueue;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
@Slf4j
public class TopicService {

    private ServiceBusConfigService configService;
    private ClientService clientService;

    public void sendMessage(String topicName, String message) {
        ServiceBusSenderClient senderClient = configService
                .clientBuilder()
                .sender()
                .topicName(topicName)
                .buildClient();
        senderClient.sendMessage(new ServiceBusMessage(message));
    }

    @SneakyThrows
    public void processMessages(String topicName, String subscriptionName, int processingSecs) {
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(context -> processMessage(topicName, subscriptionName, context))
                .processError(context -> processError(topicName, subscriptionName, context))
                .buildProcessorClient();

        log.info("Starting the processor for {}", subscriptionName);
        processorClient.start();

        TimeUnit.SECONDS.sleep(processingSecs);
        log.info("Stopping and closing the processor for {}", subscriptionName);
        processorClient.close();
    }

    @SneakyThrows
    public int countDeadLetterQueue(String topicName, String subscriptionName, int processingSecs) {
        int count = 0;
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .processMessage(context -> peekMessage(topicName, subscriptionName, context, count))
                .processError(context -> processError(topicName, subscriptionName, context))
                .buildProcessorClient();

        log.info("Starting the processor for {}", subscriptionName);
        processorClient.start();

        TimeUnit.SECONDS.sleep(processingSecs);
        log.info("Stopping and closing the processor for {} with {} processed", subscriptionName, count);
        processorClient.close();
        return count;
    }

    @SneakyThrows
    public void processDeadLetterQueue(String topicName, String subscriptionName, int processingSecs) {
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .processMessage(context -> processMessage(topicName, subscriptionName, context))
                .processError(context -> processError(topicName, subscriptionName, context))
                .buildProcessorClient();

        log.info("Starting the processor for {}", subscriptionName);
        processorClient.start();

        TimeUnit.SECONDS.sleep(processingSecs);
        log.info("Stopping and closing the processor for {}", subscriptionName);
        processorClient.close();
    }

    private void peekMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context, int count) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Peeking {}/{} messageId:{} {}", topicName, subscriptionName, message.getMessageId(), message.getBody());
        count++;
    }

    private void processMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing {}/{} messageId:{} {}", topicName, subscriptionName, message.getMessageId(), message.getBody());
        clientService.receiveMessage(topicName, subscriptionName, String.valueOf(message.getBody()));
    }

    private static void processError(String topicName, String subscriptionName, ServiceBusErrorContext context) {
        // We need to properly handle the error ... leave it on the queue / send to DLQ
        log.error("error processing subscription message.", context.getException());
    }
}
