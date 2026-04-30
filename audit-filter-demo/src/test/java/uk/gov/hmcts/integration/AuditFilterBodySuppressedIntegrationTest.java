package uk.gov.hmcts.integration;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "audit.http.include-payload-body=false")
class AuditFilterBodySuppressedIntegrationTest extends AuditFilterIntegrationTestBase {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void audit_payload_should_contain_path_and_query_params_but_not_body_when_body_capture_is_disabled() throws Exception {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Map<String, Object>> request = new HttpEntity<>(
            Map.of("documentType", "CLAIM_FORM", "filename", "claim.pdf"), headers);

        restTemplate.postForEntity(
            "http://localhost:" + port + "/cases/CASE-001/documents?caseType=CIVIL",
            request, String.class);

        Thread.sleep(1000);

        final List<JsonNode> messages = drainAuditQueue();
        log.info("Received {} audit message(s)", messages.size());
        messages.forEach(m -> log.info("Audit message: {}", m));

        assertThat(messages).isNotEmpty();

        final JsonNode content = messages.get(0).path("content");
        log.info("Request audit content: {}", content);

        assertThat(content.path("caseId").asText()).isEqualTo("CASE-001");  // path param — present
        assertThat(content.path("caseType").asText()).isEqualTo("CIVIL");   // query param — present
        assertThat(content.has("documentType")).isFalse();                  // body — suppressed
        assertThat(content.has("filename")).isFalse();                      // body — suppressed
        assertThat(content.has("_metadata")).isTrue();
    }
}
