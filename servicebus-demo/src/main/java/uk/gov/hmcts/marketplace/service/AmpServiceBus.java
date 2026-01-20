package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class AmpServiceBus {
    private final ServiceBusSenderClient senderClient;
    private final ServiceBusReceiverClient receiverClient;

    public void sendMessage(String message) {
        senderClient.sendMessage(new ServiceBusMessage(message));
        log.info("Sent message to queue {}", senderClient.getIdentifier());
        senderClient.close();
    }

    public List<String> getMessages(int maxMessages) {
        List<String> messages = new ArrayList<>();
        receiverClient.receiveMessages(maxMessages).forEach(msg -> {
            messages.add(String.valueOf(msg.getBody()));
            receiverClient.complete(msg);
        });
        receiverClient.close();
        return messages;
    }
}
