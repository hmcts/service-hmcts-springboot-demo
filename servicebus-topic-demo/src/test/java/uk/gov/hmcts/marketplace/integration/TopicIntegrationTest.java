package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.administration.models.CreateSubscriptionOptions;
import com.azure.messaging.servicebus.administration.models.CreateTopicOptions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.marketplace.service.AmpClientService;
import uk.gov.hmcts.marketplace.service.TopicService;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest
public class TopicIntegrationTest extends TopicIntegrationTestBase {

    @Autowired
    TopicService topicService;

    @MockitoBean
    AmpClientService ampClientService;

    @BeforeEach
    void setUp() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
        createTopicAndSubscription(topicName, subscription1);
        createTopicAndSubscription(topicName, subscription2);
        purgeMessages(topicName, subscription1);
        purgeMessages(topicName, subscription2);
    }

    @Test
    void sent_messages_should_process_and_send_to_client() {
        String message1 = randomMessage();
        topicService.sendMessage(topicName, message1);
        String message2 = randomMessage();
        topicService.sendMessage(topicName, message2);

        topicService.processMessages(topicName, subscription1, 500);

        verify(ampClientService).receiveMessage(topicName, subscription1, message1);
        verify(ampClientService).receiveMessage(topicName, subscription1, message2);
    }

    @Test
    void process_message_should_retry_n_times_then_send_to_DLQ() {
        topicService.sendMessage(topicName, message);

        log.info("getting messages ... {} sends and then should fail to DLQ", maxDeliveryCount);
        doThrow(HttpClientErrorException.class).when(ampClientService).receiveMessage(topicName, subscription1, message);
        topicService.processMessages(topicName, subscription1, 500);
        verify(ampClientService, times(maxDeliveryCount)).receiveMessage(topicName, subscription1, message);

        log.info("reprocessing messages from DLQ just once");
        reset(ampClientService);
        topicService.processDeadLetterMessages(topicName, subscription1, 500);
        verify(ampClientService).receiveMessage(topicName, subscription1 + "-DLQ", message);
    }

    @SneakyThrows
    @Test
    void many_threads_reading_many_messages_should_be_ok() {
        int numberOfMessages = 100;
        int numberOfProcessors = 7;
        int processingMilliSecs = 10000;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfProcessors);
        createProcessorThreads(executor, numberOfProcessors, processingMilliSecs);
        for (int n = 0; n < numberOfMessages; n++) {
            log.info("Sending message {}", n);
            topicService.sendMessage(topicName, "My message" + n);
        }
        log.info("Sent {} messages", numberOfMessages);
        TimeUnit.MILLISECONDS.sleep(processingMilliSecs);
        executor.shutdown();

        verify(ampClientService, times(numberOfMessages)).receiveMessage(eq(topicName), eq(subscription1), anyString());
    }


    private void createProcessorThreads(ExecutorService executorService, int numberOfProcessors, int processingMilliSecs) {
        for (int n = 0; n < numberOfProcessors; n++) {
            executorService.submit(() -> {
                topicService.processMessages(topicName, subscription1, processingMilliSecs);
                return true;
            });
        }
    }

    private String randomMessage() {
        return String.format("My message %04d", new Random().nextInt(1000));
    }

    private void createTopicAndSubscription(String topicName, String subscriptionName) {
        if (!adminClient.getTopicExists(topicName)) {
            CreateTopicOptions createTopicOptions = new CreateTopicOptions();
            createTopicOptions.setDefaultMessageTimeToLive(Duration.ofHours(1));
            createTopicOptions.setDuplicateDetectionRequired(false);
            adminClient.createTopic(topicName, createTopicOptions);
        }
        if (!adminClient.getSubscriptionExists(topicName, subscriptionName)) {
            CreateSubscriptionOptions options = new CreateSubscriptionOptions();
            options.setDeadLetteringOnMessageExpiration(true);
            options.setDefaultMessageTimeToLive(Duration.ofHours(1));
            options.setLockDuration(Duration.ofMinutes(1));
            options.setMaxDeliveryCount(3);
            adminClient.createSubscription(topicName, subscriptionName, options);
        }
    }
}
