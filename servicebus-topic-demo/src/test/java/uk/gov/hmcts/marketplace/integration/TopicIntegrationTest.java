package uk.gov.hmcts.marketplace.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.marketplace.service.ClientService;
import uk.gov.hmcts.marketplace.service.TopicService;

import java.util.Random;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest
public class TopicIntegrationTest {

    @Autowired
    TopicService topicService;

    @MockitoBean
    ClientService clientService;

    @Test
    void sent_message_should_process_and_send_to_two_clients() {
        String message = String.format("My message %04d", new Random().nextInt(100));
        topicService.sendMessage(message);
        topicService.sendMessage(message);

        topicService.processMessages("subscription.1", 2);
        verify(clientService, times(2)).receiveMessage("subscription.1", message);

        topicService.processMessages("subscription.2", 2);
        verify(clientService, times(2)).receiveMessage("subscription.2", message);
    }
}
