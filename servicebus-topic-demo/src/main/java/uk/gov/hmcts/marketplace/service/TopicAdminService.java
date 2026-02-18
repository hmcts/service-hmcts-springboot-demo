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
}
