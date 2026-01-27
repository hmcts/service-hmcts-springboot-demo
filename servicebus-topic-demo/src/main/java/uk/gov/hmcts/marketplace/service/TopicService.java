package uk.gov.hmcts.marketplace.service;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
@Slf4j
public class TopicService {

    private ServiceBusConfigService configService;
    private ClientService clientService;

    public void sendMessage(String message) {
        ServiceBusSenderClient senderClient = configService
                .clientBuilder()
                .sender()
                .topicName("topic.1")
                .buildClient();
        senderClient.sendMessage(new ServiceBusMessage(message));
    }

    @SneakyThrows
    public void processMessages(int processingSecs) {
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .topicName("topic.1")
                .subscriptionName("subscription.1")
                .processMessage(context -> processMessage(context))
                .processError(context -> processError(context))
                .buildProcessorClient();

        System.out.println("Starting the processor");
        processorClient.start();

        TimeUnit.SECONDS.sleep(processingSecs);
        System.out.println("Stopping and closing the processor");
        processorClient.close();
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing messageId:{} {}", message.getMessageId(), message.getBody());
        clientService.receiveMessage(String.valueOf(message.getBody()));
    }

    private static void processError(ServiceBusErrorContext context) {
        // We need to properly handle the error ... leave it on the queue / send to DLQ
        log.error("error processing subscription message.", context.getException());
    }
}
