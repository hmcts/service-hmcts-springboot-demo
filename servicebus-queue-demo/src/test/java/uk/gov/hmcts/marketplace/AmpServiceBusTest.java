package uk.gov.hmcts.marketplace;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;
import uk.gov.hmcts.marketplace.service.AmpServiceBus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.marketplace.service.AmpServiceBus.QUEUE_NAME;

@ExtendWith(MockitoExtension.class)
class AmpServiceBusTest {
    @Mock
    ServiceBusConfigService serviceBusConfigService;

    @InjectMocks
    AmpServiceBus ampServiceBus;

    @Captor
    ArgumentCaptor<ServiceBusMessage> messageCaptor;
    @Mock
    ServiceBusReceivedMessage mockReceivedMessage;
    @Mock
    ServiceBusSenderClient senderClient;
    @Mock
    ServiceBusReceiverClient receiverClient;

    @Test
    void sb_should_send_messages() {
        when(serviceBusConfigService.serviceBusSender(QUEUE_NAME)).thenReturn(senderClient);
        ampServiceBus.sendMessage("My message");
        verify(senderClient).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getBody().toString()).isEqualTo("My message");
        verify(senderClient).close();
    }

    @Test
    void sb_should_receive_empty_messages() {
        when(serviceBusConfigService.serviceBusReceiver(QUEUE_NAME)).thenReturn(receiverClient);
        when(receiverClient.receiveMessages(2, Duration.ofSeconds(2))).thenReturn(new IterableStream<>(new ArrayList<>()));
        List<String> messages = ampServiceBus.getMessages(2, Duration.ofSeconds(2));
        assertThat(messages.size()).isEqualTo(0);
        verify(receiverClient).close();
    }

    @Test
    void sb_should_receive_messages() {
        when(serviceBusConfigService.serviceBusReceiver(QUEUE_NAME)).thenReturn(receiverClient);
        ArrayList<ServiceBusReceivedMessage> response = new ArrayList<>(Arrays.asList(mockReceivedMessage));
        when(receiverClient.receiveMessages(2, Duration.ofSeconds(2))).thenReturn(new IterableStream<>(response));
        when(mockReceivedMessage.getBody()).thenReturn(BinaryData.fromString("My message"));

        List<String> messages = ampServiceBus.getMessages(2);

        assertThat(messages.size()).isEqualTo(1);
        assertThat(messages.get(0)).isEqualTo("My message");
        verify(receiverClient).complete(mockReceivedMessage);
        verify(receiverClient).close();
    }
}