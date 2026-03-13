package uk.gov.hmcts.cp.demo.audit.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: starts an Artemis broker in a Testcontainer, configures the app
 * to use it (native mode), then verifies that audit messages are sent to the
 * audit-log queue when HTTP requests are made.
 * Requires Docker to be running and reachable by Testcontainers (e.g. DOCKER_HOST set if needed).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuditToArtemisIntegrationTest {

    private static final int ARTEMIS_PORT = 61616;

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> artemis = new GenericContainer<>(
        DockerImageName.parse("apache/activemq-artemis:2.44.0-alpine"))
        .withEnv("ANONYMOUS_LOGIN", "true")
        .withEnv("BROKER_CONFIG_MAX_DISK_USAGE", "100")
        .withExposedPorts(ARTEMIS_PORT);

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JmsTemplate jmsTemplate;

    @DynamicPropertySource
    static void artemisProperties(DynamicPropertyRegistry registry) {
        String brokerUrl = "tcp://" + artemis.getHost() + ":" + artemis.getMappedPort(ARTEMIS_PORT);
        registry.add("spring.artemis.broker-url", () -> brokerUrl);
    }

    @Test
    void auditMessageIsSentToArtemisWhenRequestIsMade() throws Exception {
        jmsTemplate.setReceiveTimeout(TimeUnit.SECONDS.toMillis(10));

        restTemplate.getForEntity("http://localhost:" + port + "/", String.class);

        // Allow async appender and network to deliver the message to Artemis
        Thread.sleep(1000);

        String messageBody = (String) jmsTemplate.receiveAndConvert(ArtemisAuditConfig.AUDIT_QUEUE_NAME);
        assertThat(messageBody)
            .as("Expected one audit message on queue %s", ArtemisAuditConfig.AUDIT_QUEUE_NAME)
            .isNotNull();

        JsonNode audit = objectMapper.readTree(messageBody);

        assertThat(audit.path("loggerName").asText()).isEqualTo("AUDIT");
        assertThat(audit.has("timestamp")).isTrue();
        assertThat(audit.has("request")).isTrue();
        assertThat(audit.has("response")).isTrue();

        JsonNode request = audit.path("request");
        assertThat(request.path("method").asText()).isEqualTo("GET");
        assertThat(request.path("uri").asText()).isEqualTo("/");

        JsonNode response = audit.path("response");
        assertThat(response.path("status").asInt()).isEqualTo(HttpStatus.OK.value());
    }
}
