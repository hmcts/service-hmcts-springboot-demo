package uk.gov.hmcts.marketplace;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ServiceBus {
    private static final String CONNECTION_STRING = "Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    private static final String QUEUE_NAME = "queue.1";

    ServiceBusClientBuilder clientBuilder = new ServiceBusClientBuilder()
            .connectionString(CONNECTION_STRING);

    public void sendMessage(String message) {
        ServiceBusSenderClient senderClient = clientBuilder
                .sender()
                .queueName(QUEUE_NAME)
                .buildClient();
        senderClient.sendMessage(new ServiceBusMessage(message));
        log.info("Sent message to queue {}", QUEUE_NAME);
        senderClient.close();
    }

    public List<String> getMessages(int maxMessages) {
        ServiceBusReceiverClient receiverClient = clientBuilder
                .receiver()
                .queueName(QUEUE_NAME)
                .buildClient();
        List<String> messages = new ArrayList<>();
        receiverClient.receiveMessages(maxMessages).forEach(msg -> {
            messages.add(String.valueOf(msg.getBody()));
            receiverClient.complete(msg);
        });
        receiverClient.close();
        return messages;
    }
}
