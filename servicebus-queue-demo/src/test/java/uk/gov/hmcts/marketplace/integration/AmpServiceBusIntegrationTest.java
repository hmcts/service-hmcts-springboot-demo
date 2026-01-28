package uk.gov.hmcts.marketplace.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.marketplace.service.AmpServiceBus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AmpServiceBusIntegrationTest {

    @Autowired
    AmpServiceBus ampServiceBus;

    @Test
    void service_bus_should_spin_up_and_messages_pass_through() {
        // Wait for Service Bus emulator to be ready and queues to be created
        log.info("Waiting for Service Bus emulator to be ready and queues to be created...");
        await()
            .atMost(120, SECONDS)
            .pollInterval(1, SECONDS)
            .until(this::isInitialized);
        log.info("Service Bus is ready. Sending messages to service bus");
        ampServiceBus.sendMessage("Message One");
        ampServiceBus.sendMessage("Message Two");
        List<String> messages = ampServiceBus.getMessages(2);
        assertThat(messages.get(0)).isEqualTo("Message One");
        assertThat(messages.get(1)).isEqualTo("Message Two");
    }

    /**
     * Checks if Service Bus is ready by attempting to send and receive a test message.
     * Returns true if the operation succeeds, false otherwise.
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
            log.debug("Service Bus not ready yet: {}", e.getMessage());
            return false;
        }
    }
}
