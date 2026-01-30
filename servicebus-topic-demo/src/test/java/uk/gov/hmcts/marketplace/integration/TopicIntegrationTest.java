package uk.gov.hmcts.marketplace.integration;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.marketplace.service.ClientService;
import uk.gov.hmcts.marketplace.service.TopicService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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

    @BeforeEach
    void setUp() {
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
    }

    String topicName = "topic.1";
    String subscription1 = "subscription.1";
    String subscription2 = "subscription.2";
    int maxDeliveryCount = 3;

    @Test
    void service_bus_config_should_match_expected() {
        log.info(
                """
                           Service Bus Emulator limitations ...
                           Sadly, we cannot control the service bus config programmatically with the azure sb emulator
                            We can only go with the config file loaded in by docker-compose
                            So we need to make sure the config matches what we expect in our tests
                            ( Of course changing the config requires docker-compose restart to reload the config
                        """);
        assertServiceBusConfigFile();
    }

    @Test
    void sent_messages_should_process_and_send_to_client() {
        String message1 = String.format("My message %04d", new Random().nextInt(100));
        topicService.sendMessage(topicName, message1);
        String message2 = String.format("My message %04d", new Random().nextInt(100));
        topicService.sendMessage(topicName, message2);

        topicService.processMessages(topicName, subscription1, 2);
        verify(clientService).receiveMessage(topicName, subscription1, message1);
        verify(clientService).receiveMessage(topicName, subscription1, message2);
    }

    @Test
    void process_message_should_retry_n_times_with_x_secs_delay() {
        String message = String.format("My message %04d", new Random().nextInt(100));
        topicService.sendMessage(topicName, message);

        doThrow(HttpClientErrorException.class).when(clientService).receiveMessage(topicName, subscription1, message);
        topicService.processMessages(topicName, subscription1, 2);

        verify(clientService, times(maxDeliveryCount)).receiveMessage(topicName, subscription1, message);

//        assertThat(topicService.countDeadLetterQueue(topicName, subscription1, 2)).isEqualTo(1);
//        assertThat(topicService.countDeadLetterQueue(topicName, subscription1, 2)).isEqualTo(1);
    }

    private void assertServiceBusConfigFile() {
        String topicPath = "UserConfig.Namespaces[0].Topics[0]";

        String topicName = (String) getValueFromServiceBusConfig(topicPath + ".Name");
        assertThat(topicName).isEqualTo("topic.1");

        String subscriptionName = (String) getValueFromServiceBusConfig(topicPath + ".Subscriptions[0].Name");
        assertThat(subscriptionName).isEqualTo("subscription.1");

        int maxDeliveryCount = (int) getValueFromServiceBusConfig(topicPath + ".Subscriptions[0].Properties.MaxDeliveryCount");
        assertThat(maxDeliveryCount).isEqualTo(3);

        boolean deadLetterEnabled = (boolean) getValueFromServiceBusConfig(topicPath + ".Subscriptions[0].Properties.DeadLetteringOnMessageExpiration");
        assertThat(deadLetterEnabled).isTrue();

        log.info("Topic {} Subscription {} has maxDeliveryCount:{}", topicName, subscriptionName, maxDeliveryCount);
    }

    @SneakyThrows
    private Object getValueFromServiceBusConfig(String jsonPath) {
        String configFileJson = Files.readString(Path.of("docker/service-bus-config.json"));
        DocumentContext jsonContext = JsonPath.parse(configFileJson);
        return jsonContext.read(jsonPath);
    }

    private boolean isServiceBusReady() {
        try {
            topicService.processMessages(topicName, subscription1, 1);
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }
}
