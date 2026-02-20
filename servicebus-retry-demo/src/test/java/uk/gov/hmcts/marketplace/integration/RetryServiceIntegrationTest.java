package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.marketplace.service.RemoteClientService;
import uk.gov.hmcts.marketplace.service.TopicAdminService;
import uk.gov.hmcts.marketplace.service.TopicService;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * To keep this simple ... we require docker compose up before running the int tests
 */
@Slf4j
@SpringBootTest
@TestPropertySource(properties = {"service-bus.retry-seconds=0,1,2,600"})
class RetryServiceIntegrationTest {

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
    void success_should_process_just_once() {
        // The remote client returns OK so we try it just once
        when(remoteClientService.receiveMessage(topicName, subscriptionName, "My message")).thenReturn(200);
        topicService.queueMessage(topicName, correlationId, "My message", 0);
        wait(2000);
        verify(remoteClientService, times(1)).receiveMessage(topicName, subscriptionName, "My message");
    }

    @Test
    void failure_should_request_and_retry_with_delays() {
        // The remote client errors
        // Since we set first delay to 1 sec and second delay to 2 seconds and wait 4 seconds then we expect 3 attempts
        // i.e. 0 + 1 + 2 = 3 seconds for 3 attempts
        when(remoteClientService.receiveMessage(topicName, subscriptionName, "My message")).thenThrow(exception);
        topicService.queueMessage(topicName, correlationId, "My message", 0);
        wait(4000);
        verify(remoteClientService, times(3)).receiveMessage(topicName, subscriptionName, "My message");
    }

    @SneakyThrows
    private void wait(int milliSecs) {
        log.info("WAITING ... start");
        Thread.sleep(milliSecs);
        log.info("WAITING ... done");
    }
}