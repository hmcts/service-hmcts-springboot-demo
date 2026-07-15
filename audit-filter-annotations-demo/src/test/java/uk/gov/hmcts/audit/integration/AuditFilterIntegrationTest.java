package uk.gov.hmcts.audit.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.audit.misc.service.ClockService;
import uk.gov.hmcts.audit.misc.service.UuidService;
import uk.gov.hmcts.audit.service.AuditSenderService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.audit.filter.AuditFilter.CORRELATION_ID_HEADER;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class AuditFilterIntegrationTest {

    @Autowired  private MockMvc mockMvc;
    @MockitoBean AuditSenderService auditSenderService;
    @MockitoBean
    ClockService clockService;
    @MockitoBean
    UuidService uuidService;

    @Captor ArgumentCaptor<String> stringCaptor;

    private static final String CLIENT_SUBSCRIPTION_ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
    private static final String DOCUMENT_ID            = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String CORRELATION_ID         = "b7e23ec2-9f4a-4c2e-8f3d-1a2b3c4d5e6f";
    private static final String MATERIAL_ID            = "04e92dfe-c7a6-339d-bb2f-65a2c267abb8"; // deterministic from DOCUMENT_ID
    private static final String METADATA_ID            = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String TIMESTAMP              = "2026-01-01T00:00:00Z";

    private static final String VALID_PATH =
            "/client-subscriptions/" + CLIENT_SUBSCRIPTION_ID + "/documents/" + DOCUMENT_ID;

    @BeforeEach
    void setUp() {
        when(clockService.now()).thenReturn(Instant.parse(TIMESTAMP));
        when(uuidService.randomUUID()).thenReturn(UUID.fromString(METADATA_ID));
    }

    @Test
    void calling_noannotation_endpoint_should_block_with_403() throws Exception {
        mockMvc.perform(get("/noannotation"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Audit annotation required"));
    }

    // In our real services X-Correlation-Id is added upstream by TracingFilter, which must run before AuditFilter.
    // This test covers the guard for cases where it is absent.
    @Test
    void calling_valid_endpoint_with_missing_correlation_id_should_block_with_403() throws Exception {
        mockMvc.perform(get(VALID_PATH))
                .andExpect(status().isForbidden())
                .andExpect(content().string("X-Correlation-Id required for Audit"));
    }

    @Test
    void calling_valid_endpoint_should_produce_request_and_response_audit_payloads() throws Exception {
        mockMvc.perform(get(VALID_PATH).header(CORRELATION_ID_HEADER, CORRELATION_ID))
                .andExpect(status().isOk());

        verify(auditSenderService, times(2)).send(stringCaptor.capture());
        final List<String> sent = stringCaptor.getAllValues();

        JSONAssert.assertEquals(expectedRequestPayload(),  sent.get(0), JSONCompareMode.STRICT);
        JSONAssert.assertEquals(expectedResponsePayload(), sent.get(1), JSONCompareMode.STRICT);
    }

    @Test
    void calling_excluded_endpoint_without_correlation_id_should_proceed_with_200() throws Exception {
        mockMvc.perform(get("/excluded"))
                .andExpect(status().isOk());
    }

    private String expectedRequestPayload() {
        return """
                {
                  "origin":    "hearing-results-document",
                  "component": "QUERY_API",
                  "timestamp": "%s",
                  "content": {
                    "_metadata": {
                      "id":   "%s",
                      "name": "hearing-results-document.get-document",
                      "context": { "user": "00000000-0000-0000-0000-000000000000" }
                    },
                    "eventType":      "REQUEST",
                    "action":         "Download",
                    "materialId":     null,
                    "caseId":         null,
                    "hearingId":      null,
                    "courtDocumentId": null,
                    "correlationId":  "%s",
                    "responseStatus": null,
                    "pathParams": {
                      "clientSubscriptionId": "%s",
                      "documentId":           "%s"
                    }
                  }
                }
                """.formatted(TIMESTAMP, METADATA_ID, CORRELATION_ID, CLIENT_SUBSCRIPTION_ID, DOCUMENT_ID);
    }

    private String expectedResponsePayload() {
        return """
                {
                  "origin":    "hearing-results-document",
                  "component": "QUERY_API",
                  "timestamp": "%s",
                  "content": {
                    "_metadata": {
                      "id":   "%s",
                      "name": "hearing-results-document.get-document",
                      "context": { "user": "00000000-0000-0000-0000-000000000000" }
                    },
                    "eventType":      "RESPONSE",
                    "action":         "Download",
                    "materialId":     "%s",
                    "caseId":         null,
                    "hearingId":      null,
                    "courtDocumentId": null,
                    "correlationId":  "%s",
                    "responseStatus": 200,
                    "pathParams": {
                      "clientSubscriptionId": "%s",
                      "documentId":           "%s"
                    }
                  }
                }
                """.formatted(TIMESTAMP, METADATA_ID, MATERIAL_ID, CORRELATION_ID, CLIENT_SUBSCRIPTION_ID, DOCUMENT_ID);
    }
}
