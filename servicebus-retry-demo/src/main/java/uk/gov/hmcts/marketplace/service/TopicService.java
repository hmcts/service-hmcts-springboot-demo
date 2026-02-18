package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class TopicService {

    private ServiceBusConfigService configService;
    private RetryService retryService;
    private RemoteClientService remoteClientService;

    public void queueMessage(String topicName, UUID correlationId, String message, int failCount) {
        QueueMessage queueMessage = QueueMessage.builder()
                .correlationId(correlationId)
                .failCount(failCount)
                .message(message)
                .build();
        ServiceBusSenderClient serviceBusSenderClient = configService
                .clientBuilder()
                .sender()
                .topicName(topicName)
                .buildClient();
        ServiceBusMessage serviceBusMessage = new ServiceBusMessage(queueMessage.toJson());
        serviceBusMessage.setScheduledEnqueueTime(retryService.getNextTryTime(failCount));
        serviceBusSenderClient.sendMessage(serviceBusMessage);
        serviceBusSenderClient.close();
        log.info("Queued message to topic:{} with failCount:{} and correlationId:{}", topicName, failCount, correlationId);
    }

    @SneakyThrows
    public void startMessageProcessor(String topicName, String subscriptionName) {
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> handleError(topicName, subscriptionName, context))
                .buildProcessorClient();

        process(processorClient);
    }

    public void handleMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        QueueMessage queueMessage = QueueMessage.fromJson(String.valueOf(context.getMessage().getBody()));
        log.info("Processing {}/{} correlationId:{}", topicName, subscriptionName, queueMessage.getCorrelationId());
        try {
            remoteClientService.receiveMessage(topicName, subscriptionName, queueMessage.getMessage());
        } catch (Exception e) {
            int failCount = queueMessage.getFailCount() + 1;
            queueMessage(topicName, queueMessage.getCorrelationId(), queueMessage.getMessage(), failCount);
            // Because we added a new message and swallowed the error then the current message will be dropped
        }
    }

    public void handleError(String topicName, String subscriptionName, ServiceBusErrorContext errorContext) {
        // We should never be called because we catch all in the messageHandler
        log.error("handleError unexpected error on {}/{}", topicName, subscriptionName, errorContext.getException());
    }

    @SneakyThrows
    private void process(ServiceBusProcessorClient processorClient) {
        log.info("starting service bus processor {}/{}", processorClient.getTopicName(), processorClient.getSubscriptionName());
        processorClient.start();
    }
}
