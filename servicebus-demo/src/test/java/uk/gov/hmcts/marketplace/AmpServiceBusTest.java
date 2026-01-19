package uk.gov.hmcts.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.marketplace.service.AmpServiceBus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AmpServiceBusTest {

    @InjectMocks
    AmpServiceBus ampServiceBus;

    @Test
    void sb_should_send_and_receive_messages() {
        ampServiceBus.sendMessage("Message One");
        ampServiceBus.sendMessage("Message Two");
        List<String> messages = ampServiceBus.getMessages(2);
        assertThat(messages.get(0)).isEqualTo("Message One");
        assertThat(messages.get(1)).isEqualTo("Message Two");
    }
}