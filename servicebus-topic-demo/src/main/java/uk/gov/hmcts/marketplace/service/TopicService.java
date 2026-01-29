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
    public void processMessages(String subscriptionName, int processingSecs) {
        ServiceBusProcessorClient processor = configService
                .clientBuilder()
                .processor()
                .topicName("topic.1")
                .subscriptionName(subscriptionName)
                .processMessage(context -> processMessage(subscriptionName, context))
                .processError(context -> processError(context))
                .buildProcessorClient();

        log.info("Starting processor {}", subscriptionName);
        processor.start();

        TimeUnit.SECONDS.sleep(processingSecs);
        log.info("Stopping processor {}", subscriptionName);
        processor.close();
    }

    private void processMessage(String subscriptionName, ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing messageId:{} {}", message.getMessageId(), message.getBody());
        log.info("Message has deliveryCount:{}, timeToLive:{}", message.getDeliveryCount(), message.getTimeToLive());
        clientService.receiveMessage(subscriptionName, String.valueOf(message.getBody()));
    }

    private void processError(ServiceBusErrorContext context) {
        // We need to properly handle the error ... leave it on the queue / send to DLQ
        log.error("error processing subscription message.", context.getException());
    }
}
