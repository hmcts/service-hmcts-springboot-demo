package uk.gov.hmcts.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ServiceBusTest {

    @InjectMocks
    ServiceBus serviceBus;

    @Test
    void sb_should_send_and_receive_messages() {
        serviceBus.sendMessage("Message One");
        serviceBus.sendMessage("Message Two");
        List<String> messages = serviceBus.getMessages(2);
        assertThat(messages.get(0)).isEqualTo("Message One");
        assertThat(messages.get(1)).isEqualTo("Message Two");
    }
}