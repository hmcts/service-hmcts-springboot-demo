package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class AmpServiceBus {
    public static final String QUEUE_NAME = "queue.1";

    private final ServiceBusConfigService  serviceBusConfigService;

    public void sendMessage(String message) {
        ServiceBusSenderClient sender = serviceBusConfigService.serviceBusSender(QUEUE_NAME);
        sender.sendMessage(new ServiceBusMessage(message));
        log.info("Sent message to queue {}", QUEUE_NAME);
        sender.close();
    }

    public List<String> getMessages(int maxMessages) {
        ServiceBusReceiverClient receiver = serviceBusConfigService.serviceBusReceiver(QUEUE_NAME);
        List<String> messages = new ArrayList<>();
        receiver.receiveMessages(maxMessages).forEach(msg -> {
            messages.add(String.valueOf(msg.getBody()));
            receiver.complete(msg);
        });
        receiver.close();
        log.info("Read {} messages from queue {}", messages.size(), QUEUE_NAME);
        return messages;
    }
}
