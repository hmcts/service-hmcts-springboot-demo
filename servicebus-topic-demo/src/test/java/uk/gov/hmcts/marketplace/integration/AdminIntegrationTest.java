package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.azure.messaging.servicebus.administration.models.CreateTopicOptions;
import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;
import uk.gov.hmcts.marketplace.service.TopicAdminService;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class AdminIntegrationTest extends TopicIntegrationTestBase {

    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    TopicAdminService adminService;

    @Test
    void admin_console_should_get_topics_andsubscriptions() {
        List<String> topics = configService.adminClient().listTopics().stream().map(TopicProperties::getName).toList();
        assertThat(topics).isEqualTo(List.of("topic.1"));

        List<String> subscriptions = configService.adminClient().listSubscriptions(topicName).stream().map(SubscriptionProperties::getSubscriptionName).toList();
        assertThat(subscriptions).isEqualTo(List.of("subscription.1", "subscription.2"));

        // SADLY, We cannot currently get runtime properties for a subscription in emulator. It seems that activeMessageCount and other counts are null
        // Thus it throws npe when trying to out them into int values
        // SubscriptionRuntimeProperties subscription1Properties = adminClient.getSubscriptionRuntimeProperties(topicName, subscription1);
        // log.info("properties:{}", subscription1Properties);
    }

    @Test
    void admin_console_should_create_new_topic_and_subscription() {
        CreateTopicOptions createTopicOptions = new CreateTopicOptions();
        createTopicOptions.setDefaultMessageTimeToLive(Duration.ofHours(1));
        createTopicOptions.setDuplicateDetectionRequired(false);
        configService.adminClient().createTopic("topic.new", createTopicOptions);

        CreateSubscriptionOptions options = new CreateSubscriptionOptions();
        options.setDeadLetteringOnMessageExpiration(true);
        options.setDefaultMessageTimeToLive(Duration.ofHours(1));
        options.setLockDuration(Duration.ofMinutes(1));
        options.setMaxDeliveryCount(3);
        configService.adminClient().createSubscription("topic.new", "subscription.new", options);

        configService.adminClient().deleteSubscription("topic.new", "subscription.new");
        configService.adminClient().deleteTopic("topic.new");
    }
}
