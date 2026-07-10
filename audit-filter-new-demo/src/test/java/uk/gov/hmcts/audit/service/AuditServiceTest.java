package uk.gov.hmcts.audit.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import uk.gov.hmcts.audit.model.AuditDecision;
import uk.gov.hmcts.audit.model.AuditEventType;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditDecisionService auditDecisionService;
    @Mock private AuditPayloadGenerationService auditPayloadGenerationService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @Mock private HandlerMethod handlerMethod;

    @InjectMocks
    private AuditService auditService;

    @Test
    void proceed_audited_should_audit_request_then_chain_then_audit_response() throws Exception {
        when(auditDecisionService.evaluate(handlerMethod, request)).thenReturn(AuditDecision.PROCEED_AUDITED);
        when(response.getStatus()).thenReturn(200);

        auditService.process(handlerMethod, request, response, chain);

        verify(auditPayloadGenerationService).generate(handlerMethod, request, AuditEventType.REQUEST, null);
        verify(chain).doFilter(request, response);
        verify(auditPayloadGenerationService).generate(handlerMethod, request, AuditEventType.RESPONSE, 200);
    }

    @Test
    void proceed_excluded_should_pass_through_without_audit() throws Exception {
        when(auditDecisionService.evaluate(handlerMethod, request)).thenReturn(AuditDecision.PROCEED_EXCLUDED);

        auditService.process(handlerMethod, request, response, chain);

        verify(chain).doFilter(request, response);
        verify(auditPayloadGenerationService, never()).generate(any(), any(), any(), any());
    }

    @Test
    void block_no_annotation_should_return_403() throws Exception {
        final StringWriter body = new StringWriter();
        when(auditDecisionService.evaluate(handlerMethod, request)).thenReturn(AuditDecision.BLOCK_NO_ANNOTATION);
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        auditService.process(handlerMethod, request, response, chain);

        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
        assertThat(body.toString()).isEqualTo("Audit annotation required");
    }

    @Test
    void block_missing_correlation_id_should_return_403() throws Exception {
        final StringWriter body = new StringWriter();
        when(auditDecisionService.evaluate(handlerMethod, request)).thenReturn(AuditDecision.BLOCK_MISSING_CORRELATION_ID);
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        auditService.process(handlerMethod, request, response, chain);

        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
        assertThat(body.toString()).isEqualTo("X-Correlation-Id required for Audit");
    }

    @Test
    void request_audit_exception_should_block_with_403_and_not_call_chain() throws Exception {
        final StringWriter body = new StringWriter();
        when(auditDecisionService.evaluate(handlerMethod, request)).thenReturn(AuditDecision.PROCEED_AUDITED);
        when(auditPayloadGenerationService.generate(eq(handlerMethod), eq(request), eq(AuditEventType.REQUEST), eq(null)))
                .thenThrow(new RuntimeException("payload generation failed"));
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        auditService.process(handlerMethod, request, response, chain);

        verify(response).setStatus(403);
        verify(chain, never()).doFilter(request, response);
    }
}
