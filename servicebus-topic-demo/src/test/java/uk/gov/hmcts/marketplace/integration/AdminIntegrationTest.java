package uk.gov.hmcts.marketplace.integration;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.azure.messaging.servicebus.administration.models.CreateTopicOptions;
import com.azure.messaging.servicebus.administration.models.SubscriptionProperties;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class AdminIntegrationTest extends TopicIntegrationTestBase {

    private int adminConnectionPort = 5300;
    private String adminConnectionString = "Endpoint=sb://127.0.0.1:5300;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

    HttpClient adminHttpClient = new NettyAsyncHttpClientBuilder()
            .port(adminConnectionPort)
            .build();

    HttpPipelinePolicy forceHttpPolicy = (context, next) -> {
        try {
            URL current = context.getHttpRequest().getUrl();
            URL httpUrl = new URL("http", current.getHost(), adminConnectionPort, current.getFile());
            context.getHttpRequest().setUrl(httpUrl);
        } catch (MalformedURLException e) {
            return Mono.error(e);
        }
        return next.process();
    };

    ServiceBusAdministrationClient adminClient = new ServiceBusAdministrationClientBuilder()
            .connectionString(adminConnectionString)
            .httpClient(adminHttpClient)
            .addPolicy(forceHttpPolicy)
            .buildClient();

    @Test
    void admin_console_should_get_topics_andsubscriptions() {
        List<String> topics = adminClient.listTopics().stream().map(TopicProperties::getName).toList();
        assertThat(topics).isEqualTo(List.of("topic.1"));

        List<String> subscriptions = adminClient.listSubscriptions(topicName).stream().map(SubscriptionProperties::getSubscriptionName).toList();
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
        adminClient.createTopic("topic.new", createTopicOptions);

        CreateSubscriptionOptions options = new CreateSubscriptionOptions();
        options.setDeadLetteringOnMessageExpiration(true);
        options.setDefaultMessageTimeToLive(Duration.ofHours(1));
        options.setLockDuration(Duration.ofMinutes(1));
        options.setMaxDeliveryCount(3);
        adminClient.createSubscription("topic.new", "subscription.new", options);

        adminClient.deleteSubscription("topic.new", "subscription.new");
        adminClient.deleteTopic("topic.new");
    }
}
