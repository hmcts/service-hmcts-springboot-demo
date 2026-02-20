package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.azure.messaging.servicebus.administration.models.CreateTopicOptions;
import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import com.azure.messaging.servicebus.models.SubQueue;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
@Slf4j
public class TopicAdminService {

    ServiceBusConfigService configService;

    public boolean isServiceBusReady() {
        try {
            configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }

    public void createTopicAndSubscription(String topicName, String subscriptionName) {
        ServiceBusAdministrationClient adminClient = configService.adminClient();
        List<String> topics = adminClient.listTopics().stream().map(TopicProperties::getName).toList();
        if (topics.contains(topicName)) {
            log.info("Topic {} already exists", topicName);
        } else {
            log.info("Creating topic {}", topicName);
            CreateTopicOptions createTopicOptions = new CreateTopicOptions();
            createTopicOptions.setDefaultMessageTimeToLive(Duration.ofHours(1));
            createTopicOptions.setDuplicateDetectionRequired(false);
            adminClient.createTopic(topicName, createTopicOptions);
        }

        List<String> subscriptions = adminClient.listSubscriptions(topicName).stream().map(SubscriptionProperties::getSubscriptionName).toList();
        if (subscriptions.contains(subscriptionName)) {
            log.info("Subscription {}/{} already exists", topicName, subscriptionName);
        } else {
            CreateSubscriptionOptions options = new CreateSubscriptionOptions();
            options.setDeadLetteringOnMessageExpiration(true);
            options.setDefaultMessageTimeToLive(Duration.ofHours(1));
            options.setLockDuration(Duration.ofMinutes(1));
            options.setMaxDeliveryCount(1);
            adminClient.createSubscription(topicName, subscriptionName, options);
        }
    }

    @SneakyThrows
    public void purgeMessages(String topicName, String subscriptionName, boolean deadLetterQueue) {
        log.info("purgeMessages {}/{} DLQ:{}", topicName, subscriptionName, deadLetterQueue);
        configService.processorClientBuilder(topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = configService.processorClientBuilder(topicName, subscriptionName);
        if(deadLetterQueue){
            processorBuilder.subQueue(SubQueue.DEAD_LETTER_QUEUE);
        }
        processorBuilder.processMessage(context -> purgeMessage(topicName, subscriptionName, context));
        processorBuilder.processError(context -> handleError(topicName, subscriptionName, context));

        ServiceBusProcessorClient processor = processorBuilder.buildProcessorClient();
        processor.start();
        TimeUnit.MILLISECONDS.sleep(100);
        processor.stop();
    }

    private void purgeMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        log.info("purgeMessage {}/{} {}", topicName, subscriptionName, context.getMessage().getMessageId());
        // do nothing just let the message complete
    }

    private void handleError(String topicName, String subscriptionName, ServiceBusErrorContext errorContext) {
        // We should never be called because we catch all in the messageHandler
        log.error("handleError unexpected error on {}/{}", topicName, subscriptionName, errorContext.getException());
    }
}
