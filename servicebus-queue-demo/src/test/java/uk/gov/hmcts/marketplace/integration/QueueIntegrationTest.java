package uk.gov.hmcts.marketplace.integration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.marketplace.service.AmpClientService;
import uk.gov.hmcts.marketplace.service.QueueService;

import java.time.Duration;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class QueueIntegrationTest extends QueueIntegrationTestBase {

    @Autowired
    QueueService queueService;
    @MockitoBean
    AmpClientService ampClientService;

    @BeforeEach
    void beforeEach() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
        purgeMessages(queueName);
    }

    @Test
    void sent_messages_should_process_and_send_to_client() {
        queueService.sendMessage(queueName, "Message One");
        queueService.sendMessage(queueName, "Message Two");
        // why does count not work ??
        // assertThat(countMessages(queueName)).isEqualTo(1);

        queueService.processMessages(queueName, 500);

        verify(ampClientService).receiveMessage(queueName, "Message One");
        verify(ampClientService).receiveMessage(queueName, "Message Two");
    }

    @Test
    void process_message_should_retry_n_times_then_send_to_DLQ() {
        queueService.sendMessage(queueName, message);

        log.info("getting messages ... 3 sends and then should fail to DLQ");
        doThrow(HttpClientErrorException.class).when(ampClientService).receiveMessage(queueName, message);
        queueService.processMessages(queueName, 500);
        verify(ampClientService, times(3)).receiveMessage(queueName, message);

        log.info("reprocessing messages from DLQ just once");
        reset(ampClientService);
        queueService.processDeadLetterMessages(queueName, 500);
        verify(ampClientService).receiveMessage(queueName + "-DLQ", message);
    }

    @SneakyThrows
    @Test
    void many_threads_reading_many_messages_should_be_ok() {
        int numberOfMessages = 100;
        int numberOfProcessors = 7;
        int processingMilliSecs = 10000;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfProcessors);
        createProcessorThreads(executorService, numberOfProcessors, processingMilliSecs);
        for (int n = 0; n < numberOfMessages; n++) {
            queueService.sendMessage(queueName, "My message" + n);
        }
        log.info("Sent {} messages", numberOfMessages);
        TimeUnit.MILLISECONDS.sleep(processingMilliSecs);
        executorService.shutdown();

        verify(ampClientService, times(numberOfMessages)).receiveMessage(eq(queueName), anyString());
    }

    private void createProcessorThreads(ExecutorService executorService, int numberOfProcessors, int processingMilliSecs) {
        for (int n = 0; n < numberOfProcessors; n++) {
            executorService.submit(() -> {
                queueService.processMessages(queueName, processingMilliSecs);
                return true;
            });
        }
    }
}
