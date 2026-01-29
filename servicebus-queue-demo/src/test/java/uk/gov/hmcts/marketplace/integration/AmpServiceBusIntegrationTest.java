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
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AmpServiceBusIntegrationTest {

    @Autowired
    AmpServiceBus ampServiceBus;
        await()
                .atMost(SERVICE_BUS_READY_TIMEOUT)
                .pollInterval(POLL_INTERVAL)
                .until(this::isServiceBusReady);
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
                .hasSize(3)
                .containsExactly(messageOne, messageTwo);
    }

    @Test
    void should_send_multiple_messages_and_preserve_order() {
        String messageOne = "Message One";
        String messageTwo = "Message Two";
        String messageThree = "Message Three";

        ampServiceBus.sendMessage(messageOne);
        ampServiceBus.sendMessage(messageTwo);
        ampServiceBus.sendMessage(messageThree);

        List<String> received = ampServiceBus.getMessages(3);
        assertThat(received)
                .hasSize(3)
                .containsExactly(messageOne, messageTwo, messageThree);
    }

    /**
     */
    private boolean isInitialized() {
        try {
            String testMessage = "health-check-" + System.currentTimeMillis();
            ampServiceBus.sendMessage(testMessage);
            List<String> messages = ampServiceBus.getMessages(1);
            boolean isServiceBusInitialised = !messages.isEmpty() && messages.get(0).equals(testMessage);
            if (isServiceBusInitialised) {
                log.debug("Service Bus is ready");
            }
            return isServiceBusInitialised;
        } catch (Exception e) {
            return false;
        }
    }
}
