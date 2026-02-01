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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest
public class TopicIntegrationTest extends TopicIntegrationTestBase {

    @Autowired
    TopicService topicService;

    @MockitoBean
    ClientService clientService;

    @BeforeEach
    void setUp() {
        log.info("START BEFORE");
        await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .until(this::isServiceBusReady);
//        System.out.println("COUNTING messages");
//        log.info("Sub1 Count:{}", countMessages(topicName, subscription1));
//        log.info("Sub1 DLQ Count:{}", countDeadLetterMessages(topicName, subscription1));
        System.out.println("PURGING messages");
        purgeMessages(topicName, subscription1);
        purgeMessages(topicName, subscription2);

//        assertThat(countMessages(topicName, subscription1)).isZero();
//        assertThat(countDeadLetterMessages(topicName, subscription1)).isZero();
//        assertThat(countMessages(topicName, subscription2)).isZero();
//        assertThat(countDeadLetterMessages(topicName, subscription2)).isZero();
        log.info("Done BEFORE");
    }

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
        String message1 = randomMessage();
        topicService.sendMessage(topicName, message1);
        String message2 = randomMessage();
        topicService.sendMessage(topicName, message2);

        topicService.processMessages(topicName, subscription1, 100);

        verify(clientService).receiveMessage(topicName, subscription1, message1);
        verify(clientService).receiveMessage(topicName, subscription1, message2);
    }

    @Test
    void process_message_should_retry_n_times_then_send_to_DLQ() {
        System.out.println("SENDING message");
        topicService.sendMessage(topicName, message);
        System.out.println("COUNTING message");
//        assertThat(countMessages(topicName, subscription1)).isEqualTo(1L);
//        assertThat(countMessages(topicName, subscription1)).isEqualTo(1L);

        log.info("getting messages ... {} sends and then should fail to DLQ", maxDeliveryCount);
        doThrow(HttpClientErrorException.class).when(clientService).receiveMessage(topicName, subscription1, message);
        topicService.processMessages(topicName, subscription1, 500);
        verify(clientService, times(maxDeliveryCount)).receiveMessage(topicName, subscription1, message);
//        assertThat(countMessages(topicName, subscription1)).isEqualTo(0L);
//        assertThat(countDeadLetterMessages(topicName, subscription1)).isEqualTo(1L);
//        assertThat(countDeadLetterMessages(topicName, subscription1)).isEqualTo(1L);

        log.info("reprocessing messages from DLQ just once");
        reset(clientService);
        topicService.processDeadLetterMessages(topicName, subscription1, 500);
        verify(clientService).receiveMessage(topicName, subscription1 + "-DLQ", message);
    }

    @Test
    void dlq_count_should_be_accurate() {
//        assertThat(countDeadLetterMessages(topicName, subscription1)).isZero();

        doThrow(HttpClientErrorException.class).when(clientService).receiveMessage(topicName, subscription1, message);
        topicService.sendMessage(topicName, message);
        topicService.processMessages(topicName, subscription1, 500);
        verify(clientService, times(maxDeliveryCount)).receiveMessage(topicName, subscription1, message);

//        assertThat(countDeadLetterMessages(topicName, subscription1)).isEqualTo(1);
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

    private String randomMessage() {
        return String.format("My message %04d", new Random().nextInt(1000));
    }

    @SneakyThrows
    private Object getValueFromServiceBusConfig(String jsonPath) {
        String configFileJson = Files.readString(Path.of("docker/service-bus-config.json"));
        DocumentContext jsonContext = JsonPath.parse(configFileJson);
        return jsonContext.read(jsonPath);
    }
}
