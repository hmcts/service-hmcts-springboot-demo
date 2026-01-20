package uk.gov.hmcts.marketplace;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.marketplace.service.AmpServiceBus;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmpServiceBusTest {

    @Mock
    ServiceBusSenderClient senderClient;
    @Mock
    ServiceBusReceiverClient receiverClient;

    @InjectMocks
    AmpServiceBus ampServiceBus;

    @Captor
    ArgumentCaptor<ServiceBusMessage> messageCaptor;

    @Test
    void sb_should_send_messages() {
        ampServiceBus.sendMessage("My message");
        verify(senderClient).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getBody().toString()).isEqualTo("My message");
        verify(senderClient).close();
    }

    @Test
    void sb_should_receive_empty_messages() {
        when(receiverClient.receiveMessages(2)).thenReturn(new IterableStream<>(new ArrayList<>()));
        List<String> messages = ampServiceBus.getMessages(2);
        assertThat(messages.size()).isEqualTo(0);
    }
}