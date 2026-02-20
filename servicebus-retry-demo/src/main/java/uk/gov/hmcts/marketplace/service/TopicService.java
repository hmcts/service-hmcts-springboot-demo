package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;
import uk.gov.hmcts.marketplace.model.QueueMessage;

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
    public ServiceBusProcessorClient startMessageProcessor(String topicName, String subscriptionName) {
        log.info("starting service bus processor {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> handleError(topicName, subscriptionName));
        ServiceBusProcessorClient processorClient = processorBuilder.buildProcessorClient();
        processorClient.start();
        return processorClient;
    }

    public void handleMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        QueueMessage queueMessage = QueueMessage.fromJson(String.valueOf(context.getMessage().getBody()));
        log.info("Processing {}/{} correlationId:{}", topicName, subscriptionName, queueMessage.getCorrelationId());
        try {
            remoteClientService.receiveMessage(topicName, subscriptionName, queueMessage.getMessage());
        } catch (Exception exception) {
            int failCount = queueMessage.getFailCount() + 1;
            log.error("handleMessage correlationId:{} failCount:{} with exception.", queueMessage.getCorrelationId(), failCount, exception);
            if (failCount >= configService.getMaxTries()) {
                log.error("handleMessage correlationId:{} failed finally", queueMessage.getCorrelationId());
                throw exception;
            }
            queueMessage(topicName, queueMessage.getCorrelationId(), queueMessage.getMessage(), failCount);
            // Because we added a new message and swallowed the error then the current message will be dropped
        }
    }

    public void handleError(String topicName, String subscriptionName) {
        // We should only be called when failCount has exceeded maxTries and message go to DLQ
        log.error("handleError unexpected error on {}/{} moving to DLQ", topicName, subscriptionName);
    }
}
