package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
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
    private RemoteClientService remoteClientService;

    /**
     * Processing messages from queue or topic-subscriber we can either
     * a) Receive with PEEK_LOCK which means we lock the message till we either abandon or complete the message
     * b) Receive with RECEIVE_AND_DELETE which means the message is locked and removed. Unless we throw exception in which
     * case the failureCount is incremented and sent to DLQ after 3 tries
     */
    public void sendMessage(String topicName, String message) {
        ServiceBusSenderClient serviceBusSenderClient = configService
                .clientBuilder()
                .sender()
                .topicName(topicName)
                .buildClient();
        serviceBusSenderClient.sendMessage(new ServiceBusMessage(message));
        serviceBusSenderClient.close();
    }

    public ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorClientBuilder(String topicName, String subscriptionName, boolean dlq, int processingMilliSecs) {
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder builder = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> handleError(topicName, subscriptionName, context));
        if (dlq) {
            builder.subQueue(SubQueue.DEAD_LETTER_QUEUE);
        }
        return builder;
    }

    @SneakyThrows
    public void processMessages(String topicName, String subscriptionName, int processingMilliSecs) {
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> handleError(topicName, subscriptionName, context))
                .buildProcessorClient();

        processFor(processorClient, processingMilliSecs);
    }

    @SneakyThrows
    public void processDeadLetterMessages(String topicName, String subscriptionName, int processingMilliSecs) {
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .processMessage(context -> handleMessage(topicName, subscriptionName + "-DLQ", context))
                .processError(context -> handleError(topicName, subscriptionName + "-DLQ", context))
                .buildProcessorClient();

        processFor(processorClient, processingMilliSecs);
    }

    public void handleMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing {}/{} messageId:{} deliveryCount:{}", topicName, subscriptionName, message.getMessageId(), message.getDeliveryCount());
        remoteClientService.receiveMessage(topicName, subscriptionName, String.valueOf(message.getBody()));
    }

    public void handleError(String topicName, String subscriptionName, ServiceBusErrorContext context) {
        // We need to properly handle the error ... leave it on the queue / send to DLQ
        log.error("error processing subscription message - {}", context.getException().getMessage());
    }

    @SneakyThrows
    private void processFor(ServiceBusProcessorClient processorClient, int processingMilliSecs) {
        log.info("starting processor");
        processorClient.start();
        TimeUnit.MILLISECONDS.sleep(processingMilliSecs);
        log.info("stopping processor");
        processorClient.close();
    }
}
