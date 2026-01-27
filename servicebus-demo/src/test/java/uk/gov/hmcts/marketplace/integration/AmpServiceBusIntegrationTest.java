package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.marketplace.service.AmpServiceBus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AmpServiceBusIntegrationTest {

    @Autowired
    AmpServiceBus ampServiceBus;

    @Value("${spring.cloud.azure.servicebus.connection-string:${AZURE_SERVICEBUS_CONNECTION_STRING:}}")
    private String connectionString;

    private static final Duration SERVICE_BUS_READY_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    @BeforeEach
    void setUp() {
        // Wait for Service Bus to be ready before running tests
        await()
                .atMost(SERVICE_BUS_READY_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .until(this::isServiceBusReady);
        // Clean up any health check messages left in the queue
        cleanupQueue();
    }

    /**
     * Cleans up any remaining messages in the queue to ensure test isolation.
     * Uses a short timeout to avoid blocking, and drains messages in batches.
     */
    private void cleanupQueue() {
        try {
            int batchSize = 10;
            int maxBatches = 5; // Safety guarding limit to prevent infinite loops
            Duration cleanupTimeout = Duration.ofMillis(500); // Short timeout to avoid blocking
            //Drain messages until queue is empty
            int totalCleanedUpMessages = Stream.generate(() -> ampServiceBus.getMessages(batchSize, cleanupTimeout))
                    .limit(maxBatches)
                    .takeWhile(batch -> !batch.isEmpty())
                    .mapToInt(List::size)
                    .sum();
            if (totalCleanedUpMessages > 0) {
                log.info("Cleaned up {} total leftover messages from queue", totalCleanedUpMessages);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup queue: {}", e.getMessage());
        }
    }
    @Test
    void should_list_topics_on_service_bus() {
        // skip if no connection string configured in env or properties
        Assumptions.assumeTrue(connectionString != null && !connectionString.isBlank(),
                "Skipping test: no Service Bus connection string configured");
        ServiceBusAdministrationClient serviceBusAdministrationClient = new ServiceBusAdministrationClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        List<String> topicNames = new ArrayList<>();
        for (TopicProperties tp : serviceBusAdministrationClient.listTopics()) {
            topicNames.add(tp.getName());
        }
        assertThat(topicNames).isNotEmpty();
    }

    @Test
    void should_list_subscriptions_for_topic() {
        String topicName = "topic.1";
        Assumptions.assumeTrue(connectionString != null && !connectionString.isBlank(),
                "Skipping test: no Service Bus connection string configured");
        Assumptions.assumeTrue(topicName != null && !topicName.isBlank(),
                "Skipping test: no topic name configured");

        ServiceBusAdministrationClient serviceBusAdministrationClient = new ServiceBusAdministrationClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        List<String> subscriptionNames = new ArrayList<>();
        serviceBusAdministrationClient.listSubscriptions(topicName).forEach(sp ->
                subscriptionNames.add(sp.getSubscriptionName())
        );
        assertThat(subscriptionNames).isNotEmpty();
    }

    @Test
    void should_send_and_receive_messages_successfully() {
        // Given
        String messageOne = "Message One";
        String messageTwo = "Message Two";

        // When
        ampServiceBus.sendMessage(messageOne);
        ampServiceBus.sendMessage(messageTwo);

        // Then
        List<String> messages = ampServiceBus.getMessages(2);
        assertThat(messages)
                .hasSize(2)
                .containsExactly(messageOne, messageTwo);
    }

    @Test
    void should_send_multiple_messages_and_preserve_order() {
        // Given
        String messageOne = "Message One";
        String messageTwo = "Message Two";
        String messageThree = "Message Three";

        // When - send multiple messages
        ampServiceBus.sendMessage(new String[]{messageOne}, new String[]{messageTwo, messageThree});

        // Then
        List<String> received = ampServiceBus.getMessages(3);
        assertThat(received)
                .hasSize(3)
                .containsExactly(messageOne, messageTwo, messageThree);
    }

    @Test
    void should_ignore_null_and_empty_messages() {
        // Given
        String validMessage = "valid message";

        // When - send null, empty, and mixed arrays
        ampServiceBus.sendMessage((String[]) null);
        ampServiceBus.sendMessage(new String[]{});
        ampServiceBus.sendMessage(new String[]{null, validMessage});

        // Then - only valid message should be received
        // Use a reasonable timeout to avoid blocking
        List<String> received = ampServiceBus.getMessages(10, Duration.ofSeconds(2));
        assertThat(received)
                .hasSize(1)
                .containsExactly(validMessage);
    }

    /**
     * Checks if Service Bus is ready by attempting to send a test message.
     * Returns true if sending succeeds, false otherwise.
     */
    private boolean isServiceBusReady() {
        try {
            String testMessage = "health-check";
            ampServiceBus.sendMessage(testMessage);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
