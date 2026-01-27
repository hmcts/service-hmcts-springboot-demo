package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    public void sendMessage(String[]... messages) {
        if (messages == null) {
            return;
        }
        Arrays.stream(messages)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .forEach(this::sendMessage);
    }

    public List<String> getMessages(int maxMessages) {
        return getMessages(maxMessages, java.time.Duration.ofSeconds(2));
    }

    public List<String> getMessages(int maxMessages, java.time.Duration maxWaitTime) {
        ServiceBusReceiverClient receiver = serviceBusConfigService.serviceBusReceiver(QUEUE_NAME);
        List<String> messages = new ArrayList<>();
        try {
            receiver.receiveMessages(maxMessages, maxWaitTime).forEach(msg -> {
                messages.add(String.valueOf(msg.getBody()));
                receiver.complete(msg);
            });
        } finally {
            receiver.close();
        }
        log.debug("Read {} messages from queue {}", messages.size(), QUEUE_NAME);
        return messages;
    }
}
