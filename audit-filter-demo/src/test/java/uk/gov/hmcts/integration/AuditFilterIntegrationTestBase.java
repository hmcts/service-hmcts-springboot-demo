package uk.gov.hmcts.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

@Testcontainers
abstract class AuditFilterIntegrationTestBase {

    static final String AUDIT_QUEUE = "jms.topic.auditing.event";
    private static final int ARTEMIS_PORT = 61616;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> artemis = new GenericContainer<>(
        DockerImageName.parse("apache/activemq-artemis:2.44.0-alpine"))
        .withEnv("ANONYMOUS_LOGIN", "true")
        .withEnv("BROKER_CONFIG_MAX_DISK_USAGE", "100")
        .withExposedPorts(ARTEMIS_PORT)
        .waitingFor(Wait.forLogMessage(".*AMQ221007.*", 1)); // wait for "Server is now live"

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        registry.add("cp.audit.hosts[0]", artemis::getHost);
        registry.add("cp.audit.port", () -> artemis.getMappedPort(ARTEMIS_PORT));
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected JmsTemplate jmsTemplate;

    protected List<JsonNode> drainAuditQueue() throws Exception {
        jmsTemplate.setReceiveTimeout(3000);
        final List<JsonNode> messages = new ArrayList<>();
        String raw;
        while ((raw = (String) jmsTemplate.receiveAndConvert(AUDIT_QUEUE)) != null) {
            messages.add(objectMapper.readTree(raw));
        }
        return messages;
    }
}
