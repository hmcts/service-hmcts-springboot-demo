package uk.gov.hmcts.marketplace.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.marketplace.service.AmpServiceBus;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AmpServiceBusIntegrationTest {

    @Autowired
    AmpServiceBus ampServiceBus;

    @BeforeEach
    void beforeEach() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
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

    private boolean isServiceBusReady() {
        try {
            ampServiceBus.getMessages(1);
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }
}
