package uk.gov.hmcts.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.jms.AuditMessageConsumer;

import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "cp.audit.jms.reconnect-attempts=0",
        "cp.audit.jms.initial-connect-attempts=3"
    }
)
class AuditFilterIntegrationTest extends AuditFilterIntegrationTestBase {

    @MockitoBean
    AuditMessageConsumer auditMessageConsumer;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void posting_to_case_documents_should_produce_request_and_response_audit_payloads() throws Exception {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        final HttpEntity<Map<String, Object>> request = new HttpEntity<>(
            Map.of("documentType", "CLAIM_FORM", "filename", "claim.pdf"), headers);

        restTemplate.postForEntity(
            "http://localhost:" + port + "/cases/CASE-001/documents?caseType=CIVIL",
            request, String.class);

        final List<String> messages = drainAuditQueue(2);
        log.info("Received {} audit message(s)", messages.size());
        messages.forEach(m -> log.info("Audit message: {}", m));

        JSONAssert.assertEquals(expectedRequestPayload(), messages.get(0), JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expectedResponsePayload(), messages.get(1), JSONCompareMode.LENIENT);
    }

    private String expectedRequestPayload() {
        return """
                {
                  "content": {
                    "caseId":       "CASE-001",
                    "caseType":     "CIVIL",
                    "documentType": "CLAIM_FORM",
                    "filename":     "claim.pdf",
                    "_metadata": {}
                  }
                }
                """;
    }

    private String expectedResponsePayload() {
        return """
                {
                  "content": {
                    "caseId":   "CASE-001",
                    "caseType": "CIVIL",
                    "document": {
                      "documentType": "CLAIM_FORM",
                      "filename":     "claim.pdf"
                    },
                    "_metadata": {}
                  }
                }
                """;
    }
}
