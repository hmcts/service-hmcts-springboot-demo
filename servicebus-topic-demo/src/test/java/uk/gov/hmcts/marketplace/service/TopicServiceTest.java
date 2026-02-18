package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    ServiceBusConfigService configService;

    @InjectMocks
    TopicService topicService;

    @Mock
    ServiceBusClientBuilder clientBuilder;
    @Mock
    ServiceBusClientBuilder.ServiceBusSenderClientBuilder senderClientBuilder;
    @Mock
    ServiceBusSenderClient senderClient;

    @Test
    void send_message_should_do_so() {
        when(configService.clientBuilder()).thenReturn(clientBuilder);
        when(clientBuilder.sender()).thenReturn(senderClientBuilder);
        when(senderClientBuilder.topicName("t")).thenReturn(senderClientBuilder);
        when(senderClientBuilder.buildClient()).thenReturn(senderClient);

        topicService.sendMessage("t", "message");

        verify(senderClient).sendMessage(any(ServiceBusMessage.class));
        verify(senderClient).close();
    }
}