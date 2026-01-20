package uk.gov.hmcts.marketplace.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
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
    void service_bus_should_spin_up_and_messages_pass_through(){
        log.info("COLINCOLIN Starting");
        ampServiceBus.sendMessage("Message One");
        ampServiceBus.sendMessage("Message Two");
        List<String> messages = ampServiceBus.getMessages(2);
        assertThat(messages.get(0)).isEqualTo("Message One");
        assertThat(messages.get(1)).isEqualTo("Message Two");
        log.info("COLINCOLIN Finished");
    }
}
