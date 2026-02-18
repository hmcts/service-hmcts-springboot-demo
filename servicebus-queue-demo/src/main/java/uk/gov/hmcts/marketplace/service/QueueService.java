package uk.gov.hmcts.marketplace.service;

import com.azure.core.amqp.AmqpRetryMode;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.SubQueue;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
@Service
public class QueueService {
    private final ServiceBusConfigService configService;
    private AmpClientService ampClientService;

    public void sendMessage(String queueName, String message) {
        ServiceBusSenderClient serviceBusSenderClient = configService
                .clientBuilder()
                .sender()
                .queueName(queueName)
                .buildClient();
        serviceBusSenderClient.sendMessage(new ServiceBusMessage(message));
        serviceBusSenderClient.close();
        log.info("Sent message to queue {}", queueName);
    }

    public ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorClientBuilder(String queueName, boolean dlq) {
        AmqpRetryOptions amqpRetryOptions = new AmqpRetryOptions();
        amqpRetryOptions.setDelay(Duration.ofSeconds(1));
        amqpRetryOptions.setMaxRetries(1);
        amqpRetryOptions.setMaxDelay(Duration.ofSeconds(15));
        amqpRetryOptions.setMode(AmqpRetryMode.EXPONENTIAL);
        amqpRetryOptions.setTryTimeout(Duration.ofSeconds(5));
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder builder = configService
                .clientBuilder()
                .retryOptions(amqpRetryOptions)
                .processor()
                .queueName(queueName)
                .processMessage(context -> handleMessage(queueName, context))
                .processError(context -> handleError(queueName, context));
        if (dlq) {
            builder.subQueue(SubQueue.DEAD_LETTER_QUEUE);
        }
        return builder;
    }

    @SneakyThrows
    public void processMessages(String queueName, int processingMilliSecs) {
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .queueName(queueName)
                .processMessage(context -> handleMessage(queueName, context))
                .processError(context -> handleError(queueName, context))
                .buildProcessorClient();

        processFor(processorClient, processingMilliSecs);
    }

    @SneakyThrows
    public void processDeadLetterMessages(String queueName, int processingMilliSecs) {
        ServiceBusProcessorClient processorClient = configService
                .clientBuilder()
                .processor()
                .queueName(queueName)
                .subQueue(SubQueue.DEAD_LETTER_QUEUE)
                .processMessage(context -> handleMessage(queueName + "-DLQ", context))
                .processError(context -> handleError(queueName + "-DLQ", context))
                .buildProcessorClient();

        processFor(processorClient, processingMilliSecs);
    }

    public void handleMessage(String queueName, ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        log.info("Processing {} messageId:{} deliveryCount:{}", queueName, message.getMessageId(), message.getDeliveryCount());
        ampClientService.receiveMessage(queueName, String.valueOf(message.getBody()));
    }

    public void handleError(String queueName, ServiceBusErrorContext context) {
        // We need to properly handle the error ... leave it on the queue / send to DLQ
        log.error("error processing subscription message - {}", context.getException().getMessage());
    }

    @SneakyThrows
    private void processFor(ServiceBusProcessorClient processorClient, int processingMilliSecs) {
        log.info("starting processor");
        processorClient.start();
        TimeUnit.MILLISECONDS.sleep(processingMilliSecs);
        log.info("stopping processor");
        processorClient.close();
    }
}
