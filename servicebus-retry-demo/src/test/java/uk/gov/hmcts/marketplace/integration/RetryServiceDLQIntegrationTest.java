package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.SubQueue;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;
import uk.gov.hmcts.marketplace.model.QueueMessage;
import uk.gov.hmcts.marketplace.service.RemoteClientService;
import uk.gov.hmcts.marketplace.service.TopicAdminService;
import uk.gov.hmcts.marketplace.service.TopicService;

import java.util.UUID;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
        // We have max tries of 1 so goes to DLQ after first try
        "service-bus.retry-seconds=0",
        "service-bus.max-tries=1"})
class RetryServiceDLQIntegrationTest {

    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    TopicAdminService adminService;
    @Autowired
    TopicService topicService;

    @MockitoBean
    RemoteClientService remoteClientService;

    String topicName = "apim.topic";
    String subscriptionName = "hmps.subscription";
    UUID correlationId = UUID.fromString("25079a62-8e0a-46db-99c5-be948ffaf8e6");
    RuntimeException exception = new RuntimeException("mocked no response from remote");
    ServiceBusProcessorClient processorClient;

    @BeforeEach
    void beforeEach() {
        if (adminService.isServiceBusReady()) {
            log.info("ServiceBus is up and running");
        } else {
            throw new RuntimeException("ServiceBus is not running. Please start it in docker-compose");
        }
        adminService.createTopicAndSubscription(topicName, subscriptionName);
        adminService.purgeMessages(topicName, subscriptionName, false);
        adminService.purgeMessages(topicName, subscriptionName, true);
        processorClient = topicService.startMessageProcessor(topicName, subscriptionName);
    }

    @AfterEach
    void afterEach() {
        if (processorClient != null && processorClient.isRunning()) {
            processorClient.stop();
        }
    }

    @Test
    void failing_message_should_goto_DLQ_after_1_try() {
        when(remoteClientService.receiveMessage(topicName, subscriptionName, "My message")).thenThrow(exception);
        topicService.queueMessage(topicName, correlationId, "My message", 0);
        wait(1000);
        verify(remoteClientService, times(1)).receiveMessage(topicName, subscriptionName, "My message");
    }

    @Test
    void failing_message_should_reprocess_from_DLQ() {
        when(remoteClientService.receiveMessage(topicName, subscriptionName, "My message")).thenThrow(exception);
        topicService.queueMessage(topicName, correlationId, "My message", 0);
        wait(1000);
        verify(remoteClientService, times(1)).receiveMessage(topicName, subscriptionName, "My message");

        reset(remoteClientService);
        when(remoteClientService.receiveMessage(topicName, subscriptionName, "My message")).thenReturn(200);
        ServiceBusProcessorClient dlqProcessorClient = startDLQMessageProcessor(topicName, subscriptionName);
        wait(1000);
        dlqProcessorClient.stop();
        verify(remoteClientService, times(1)).receiveMessage(topicName, subscriptionName, "My message");
    }

    @SneakyThrows
    private void wait(int milliSecs) {
        log.info("WAITING ... start");
        Thread.sleep(milliSecs);
        log.info("WAITING ... done");
    }

    @SneakyThrows
    private ServiceBusProcessorClient startDLQMessageProcessor(String topicName, String subscriptionName) {
        log.info("starting service bus DLQ processor {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = configService
                .clientBuilder()
                .processor()
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(context -> handleMessage(topicName, subscriptionName, context))
                .processError(context -> ignoreError());
        processorBuilder.subQueue(SubQueue.DEAD_LETTER_QUEUE);
        ServiceBusProcessorClient processorClient = processorBuilder.buildProcessorClient();
        processorClient.start();
        return processorClient;
    }

    public void handleMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        QueueMessage queueMessage = QueueMessage.fromJson(String.valueOf(context.getMessage().getBody()));
        log.info("Processing DLQ {}/{} correlationId:{}", topicName, subscriptionName, queueMessage.getCorrelationId());
        remoteClientService.receiveMessage(topicName, subscriptionName, queueMessage.getMessage());
    }

    public void ignoreError() {
    }
}