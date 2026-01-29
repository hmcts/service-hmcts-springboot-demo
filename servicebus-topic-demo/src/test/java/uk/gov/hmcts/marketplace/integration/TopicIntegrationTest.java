package uk.gov.hmcts.marketplace.integration;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.marketplace.service.ClientService;
import uk.gov.hmcts.marketplace.service.TopicService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest
public class TopicIntegrationTest {

    @Autowired
    TopicService topicService;

    @MockitoBean
    ClientService clientService;

    String topicName = "topic.1";
    String subscription1 = "subscription.1";
    String subscription2 = "subscription.2";

    @Test
    void sent_message_should_process_and_send_to_client() {
        String message = String.format("My message %04d", new Random().nextInt(100));
        topicService.sendMessage(topicName, message);

        topicService.processMessages(topicName, subscription1, 2);
        verify(clientService).receiveMessage(topicName, subscription1, message);
    }

    @Test
    void process_message_should_retry_n_times_with_x_secs_delay() {
        int messageTries = getMaxDeliveryCountFromConfig();
        String message = String.format("My message %04d", new Random().nextInt(100));
        topicService.sendMessage(topicName, message);

        doThrow(HttpClientErrorException.class).when(clientService).receiveMessage(topicName, subscription1, message);
        topicService.processMessages(topicName, subscription1, 2);

        verify(clientService, times(messageTries)).receiveMessage(topicName, subscription1, message);

//        assertThat(topicService.countDeadLetterQueue(topicName, subscription1, 2)).isEqualTo(1);
//        assertThat(topicService.countDeadLetterQueue(topicName, subscription1, 2)).isEqualTo(1);
    }

    /* Sadly, we cannot control the servic e bus config programmatically with the azure sb emulator
        We can only go with the config file loaded in by docker-compose
     */
    @SneakyThrows
    int getMaxDeliveryCountFromConfig() {
        String configFileJson = Files.readString(Path.of("docker/service-bus-config.json"));
        DocumentContext jsonContext = JsonPath.parse(configFileJson);
        String topic0 = "UserConfig.Namespaces[0].Topics[0]";
        String topicName = jsonContext.read(topic0 + ".Name");
        assertThat(topicName).isEqualTo("topic.1");
        String subscriptionName = jsonContext.read(topic0 + ".Subscriptions[0].Name");
        assertThat(subscriptionName).isEqualTo("subscription.1");
        int maxDeliveryCount = jsonContext.read(topic0 + ".Subscriptions[0].Properties.MaxDeliveryCount");
        log.info("Topic {} Subscription {} has maxDeliveryCount:{}", topicName, subscriptionName, maxDeliveryCount);
        return maxDeliveryCount;
    }
}