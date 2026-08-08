package uk.gov.hmcts.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import uk.gov.hmcts.cp.audit.model.AuditMessage;
import uk.gov.hmcts.cp.audit.service.AuditClockService;
import uk.gov.hmcts.cp.audit.service.AuditSenderService;
import uk.gov.hmcts.cp.audit.service.AuditUuidService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class AuditFilterIntegrationTest {

    private static final UUID    CASE_ID        = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final UUID    DOCUMENT_ID    = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final UUID    CORRELATION_ID = UUID.fromString("b7e23ec2-9f4a-4c2e-8f3d-1a2b3c4d5e6f");
    private static final UUID    USER_ID        = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID    METADATA_ID    = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID    MATERIAL_ID    = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant NOW            = Instant.parse("2026-01-01T00:00:00Z");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired  MockMvc mockMvc;
    @MockitoBean AuditSenderService auditSenderService;
    @MockitoBean AuditClockService  clockService;
    @MockitoBean AuditUuidService   auditUuidService;

    @Captor ArgumentCaptor<AuditMessage> messageCaptor;

    @BeforeEach
    void setUp() {
        when(clockService.now()).thenReturn(NOW);
        when(auditUuidService.randomUUID())
                .thenReturn(METADATA_ID)   // first call: metadata id for REQUEST audit
                .thenReturn(MATERIAL_ID)   // second call: materialId set by CaseDocumentService
                .thenReturn(METADATA_ID);  // third call: metadata id for RESPONSE audit
    }

    @Test
    void getting_case_document_should_produce_request_and_response_audit_messages() throws Exception {
        mockMvc.perform(get("/cases/" + CASE_ID + "/documents/" + DOCUMENT_ID)
                .header("X-Correlation-Id", CORRELATION_ID))
                .andExpect(status().isOk());

        verify(auditSenderService, times(2)).send(messageCaptor.capture());
        final List<AuditMessage> messages = messageCaptor.getAllValues();

        final String requestJson  = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(messages.get(0));
        final String responseJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(messages.get(1));
        System.out.println("REQUEST  audit:\n" + requestJson);
        System.out.println("RESPONSE audit:\n" + responseJson);

        JSONAssert.assertEquals(expectedRequest(), requestJson, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expectedResponse(), responseJson, JSONCompareMode.LENIENT);
    }

    @Test
    void getting_case_document_without_correlation_id_should_return_403() throws Exception {
        mockMvc.perform(get("/cases/" + CASE_ID + "/documents/" + DOCUMENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void getting_case_document_when_audit_send_fails_should_return_403() throws Exception {
        doThrow(new RuntimeException("Artemis unavailable")).when(auditSenderService).send(any());

        mockMvc.perform(get("/cases/" + CASE_ID + "/documents/" + DOCUMENT_ID)
                .header("X-Correlation-Id", CORRELATION_ID))
                .andExpect(status().isForbidden());
    }

    private String expectedRequest() {
        return """
                {
                  "origin":    "case-documents",
                  "component": "DOCUMENT_API",
                  "timestamp": "2026-01-01T00:00:00Z",
                  "content": {
                    "_metadata": {
                      "id":   "00000000-0000-0000-0000-000000000002",
                      "name": "case-documents.get-document",
                      "context": { "user": null }
                    },
                    "eventType":       "REQUEST",
                    "action":          "Download",
                    "correlationId":   "b7e23ec2-9f4a-4c2e-8f3d-1a2b3c4d5e6f",
                    "responseStatus":  null,
                    "materialId":      null,
                    "caseId":          null,
                    "hearingId":       null,
                    "courtDocumentId": null,
                    "pathParams": {
                      "caseId":     "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "documentId": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
                    }
                  }
                }
                """;
    }

    private String expectedResponse() {
        return """
                {
                  "origin":    "case-documents",
                  "component": "DOCUMENT_API",
                  "timestamp": "2026-01-01T00:00:00Z",
                  "content": {
                    "_metadata": {
                      "id":   "00000000-0000-0000-0000-000000000002",
                      "name": "case-documents.get-document",
                      "context": { "user": "00000000-0000-0000-0000-000000000001" }
                    },
                    "eventType":       "RESPONSE",
                    "action":          "Download",
                    "correlationId":   "b7e23ec2-9f4a-4c2e-8f3d-1a2b3c4d5e6f",
                    "responseStatus":  200,
                    "materialId":      "00000000-0000-0000-0000-000000000003",
                    "caseId":          null,
                    "hearingId":       null,
                    "courtDocumentId": null,
                    "pathParams": {
                      "caseId":     "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "documentId": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
                    }
                  }
                }
                """;
    }
}
