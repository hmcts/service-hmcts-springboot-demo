package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.administration.models.CreateQueueOptions;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import com.azure.messaging.servicebus.administration.models.QueueRuntimeProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
public class QueueAdminIntegrationTest extends QueueIntegrationTestBase {

    @BeforeEach
    void beforeEach() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
        purgeMessages(queueName);
    }

    @Test
    void admin_console_should_get_queues() {
        List<String> queues = adminClient.listQueues().stream().map(QueueProperties::getName).toList();
        assertThat(queues).isEqualTo(List.of("queue.1"));

        QueueRuntimeProperties queueProperties = adminClient.getQueueRuntimeProperties(queueName);
        log.info("activeMessageCount:{} deadLetterCount:{}", queueProperties.getActiveMessageCount(), queueProperties.getDeadLetterMessageCount());
    }

    @Test
    void admin_console_should_create_new_queue() {
        CreateQueueOptions createQueueOptions = new CreateQueueOptions();
        createQueueOptions.setDeadLetteringOnMessageExpiration(true);
        createQueueOptions.setDefaultMessageTimeToLive(Duration.ofHours(1));
        adminClient.createQueue("queue.new", createQueueOptions);

        adminClient.deleteQueue("queue.new");
    }
}
