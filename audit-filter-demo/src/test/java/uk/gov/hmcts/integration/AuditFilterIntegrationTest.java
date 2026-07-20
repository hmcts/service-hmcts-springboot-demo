package uk.gov.hmcts.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.cp.audit.model.AuditEventType;
import uk.gov.hmcts.cp.audit.model.AuditPayload;
import uk.gov.hmcts.cp.audit.service.AuditSenderService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class AuditFilterIntegrationTest {

    @Autowired  private MockMvc mockMvc;
    @MockitoBean AuditSenderService auditSenderService;

    @Captor ArgumentCaptor<AuditPayload> payloadCaptor;

    @Test
    void getting_case_document_should_produce_request_and_response_audit_payloads() throws Exception {
        final var caseId       = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        final var documentId   = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
        final var correlationId = "b7e23ec2-9f4a-4c2e-8f3d-1a2b3c4d5e6f";

        mockMvc.perform(get("/cases/" + caseId + "/documents/" + documentId)
                .header("X-Correlation-Id", correlationId))
                .andExpect(status().isOk());

        verify(auditSenderService, times(2)).send(payloadCaptor.capture());
        final List<AuditPayload> sent = payloadCaptor.getAllValues();

        final AuditPayload request = sent.get(0);
        assertThat(request.getEventType()).isEqualTo(AuditEventType.REQUEST);
        assertThat(request.getAction()).isEqualTo("Download");
        assertThat(request.getCorrelationId()).isEqualTo(UUID.fromString(correlationId));
        assertThat(request.getMetadata().getOrigin()).isEqualTo("case-documents");
        assertThat(request.getMetadata().getComponent()).isEqualTo("DOCUMENT_API");
        assertThat(request.getMetadata().getEventName()).isEqualTo("case-documents.get-document");
        assertThat(request.getPathParams()).containsEntry("caseId", caseId);
        assertThat(request.getPathParams()).containsEntry("documentId", documentId);

        final AuditPayload response = sent.get(1);
        assertThat(response.getEventType()).isEqualTo(AuditEventType.RESPONSE);
        assertThat(response.getResponseStatus()).isEqualTo(200);
        assertThat(response.getCorrelationId()).isEqualTo(UUID.fromString(correlationId));
    }

    @Test
    void getting_case_document_without_correlation_id_should_return_403() throws Exception {
        mockMvc.perform(get("/cases/3fa85f64-5717-4562-b3fc-2c963f66afa6/documents/7c9e6679-7425-40de-944b-e07fc1f90ae7"))
                .andExpect(status().isForbidden());
    }
}
