package uk.gov.hmcts.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
class AuditFilterIntegrationTest extends AuditFilterIntegrationTestBase {

    private static final String AUDIT_QUEUE = "jms.topic.auditing.event";

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    void audit_filter_should_intercept_get_request_and_send_payload_to_artemis() throws Exception {
        jmsTemplate.setReceiveTimeout(TimeUnit.SECONDS.toMillis(10));

        restTemplate.getForEntity("http://localhost:" + port + "/", String.class);

        Thread.sleep(1000);

        String payload = (String) jmsTemplate.receiveAndConvert(AUDIT_QUEUE);
        log.info("Audit payload received from Artemis queue '{}': {}", AUDIT_QUEUE, payload);
    }

    @Test
    void audit_filter_should_intercept_post_request_and_send_body_to_artemis() throws Exception {
        jmsTemplate.setReceiveTimeout(TimeUnit.SECONDS.toMillis(10));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of("hello", "world", "foo", 42), headers);
        restTemplate.postForEntity("http://localhost:" + port + "/echo", request, String.class);

        Thread.sleep(1000);

        String payload;
        while ((payload = (String) jmsTemplate.receiveAndConvert(AUDIT_QUEUE)) != null) {
            log.info("Audit payload received from Artemis queue '{}': {}", AUDIT_QUEUE, payload);
        }
    }
}
