package uk.gov.hmcts.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.cp.audit.service.AuditSenderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "cp.audit.block-on-failure=false")
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class AuditFilterAllowAuditFailureIntegrationTest {

    @Autowired  private MockMvc mockMvc;
    @MockitoBean AuditSenderService auditSenderService;

    @Test
    void getting_case_document_when_audit_send_fails_should_return_200() throws Exception {
        doThrow(new RuntimeException("Artemis unavailable")).when(auditSenderService).send(any());

        mockMvc.perform(get("/cases/3fa85f64-5717-4562-b3fc-2c963f66afa6/documents/7c9e6679-7425-40de-944b-e07fc1f90ae7")
                .header("X-Correlation-Id", "b7e23ec2-9f4a-4c2e-8f3d-1a2b3c4d5e6f"))
                .andExpect(status().isOk());
    }
}
