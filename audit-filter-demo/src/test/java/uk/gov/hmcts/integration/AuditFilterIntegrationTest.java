package uk.gov.hmcts.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
class AuditFilterIntegrationTest extends AuditFilterIntegrationTestBase {

    private static final String AUDIT_QUEUE = "jms.topic.auditing.event";

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    void audit_filter_intercepts_request_to_root_and_sends_payload_to_artemis() throws Exception {
        jmsTemplate.setReceiveTimeout(TimeUnit.SECONDS.toMillis(10));

        restTemplate.getForEntity("http://localhost:" + port + "/", String.class);

        Thread.sleep(1000);

        String payload = (String) jmsTemplate.receiveAndConvert(AUDIT_QUEUE);
        log.info("Audit payload received from Artemis queue '{}': {}", AUDIT_QUEUE, payload);
    }
}
