package uk.gov.hmcts.marketplace.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.marketplace.service.AmpServiceBus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AmpServiceBusIntegrationTest {

    @Autowired
    AmpServiceBus ampServiceBus;

    @Test
    void service_bus_should_spin_up_and_messages_pass_through() throws InterruptedException {
        // Since--wait flag is removed to avoid healthcheck timeout, Added time for containers to start
        log.info("Waiting for Service Bus emulator to be ready and queues to be created...");
        Thread.sleep(60000);  // Wait 60 seconds for containers to start and queues to be created
        log.info("Sending messages to service bus");
        ampServiceBus.sendMessage("Message One");
        ampServiceBus.sendMessage("Message Two");
        List<String> messages = ampServiceBus.getMessages(2);
        assertThat(messages.get(0)).isEqualTo("Message One");
        assertThat(messages.get(1)).isEqualTo("Message Two");
    }
}
