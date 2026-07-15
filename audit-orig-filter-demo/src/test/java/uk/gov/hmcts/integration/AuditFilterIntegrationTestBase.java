package uk.gov.hmcts.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

@Testcontainers
abstract class AuditFilterIntegrationTestBase {

    private static final int ARTEMIS_PORT = 61616;

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> artemis = new GenericContainer<>(
        DockerImageName.parse("apache/activemq-artemis:2.44.0-alpine"))
        .withEnv("ANONYMOUS_LOGIN", "true")
        .withEnv("BROKER_CONFIG_MAX_DISK_USAGE", "100")
        .withExposedPorts(ARTEMIS_PORT);

    @DynamicPropertySource
    static void auditProperties(final DynamicPropertyRegistry registry) {
        registry.add("cp.audit.hosts[0]", artemis::getHost);
        registry.add("cp.audit.port", () -> artemis.getMappedPort(ARTEMIS_PORT));
    }

    @LocalServerPort
    protected int port;

    @Autowired
    private CaptureAuditListener captureAuditListener;

    protected List<String> drainAuditQueue(final int expectedCount) throws InterruptedException {
        return captureAuditListener.drain(expectedCount, 5);
    }
}
