package uk.gov.hmcts.audit.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import uk.gov.hmcts.audit.annotation.AuditDetail;
import uk.gov.hmcts.audit.annotation.AuditExclude;
import uk.gov.hmcts.audit.model.AuditDecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.audit.filter.AuditFilter.CORRELATION_ID_HEADER;

@ExtendWith(MockitoExtension.class)
class AuditDecisionServiceTest {

    @Mock private HandlerMethod handlerMethod;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private AuditDecisionService service;

    @BeforeEach
    void setUp() {
        when(handlerMethod.getBeanType()).thenReturn((Class) Object.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
    }

    @Test
    void handler_with_no_annotation_should_return_block_no_annotation() {
        when(handlerMethod.getMethodAnnotation(AuditExclude.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(AuditDetail.class)).thenReturn(null);

        assertThat(service.evaluate(handlerMethod, request)).isEqualTo(AuditDecision.BLOCK_NO_ANNOTATION);
    }

    @Test
    void handler_with_audit_exclude_should_return_proceed_excluded() {
        when(handlerMethod.getMethodAnnotation(AuditExclude.class)).thenReturn(auditExcludeInstance());

        assertThat(service.evaluate(handlerMethod, request)).isEqualTo(AuditDecision.PROCEED_EXCLUDED);
    }

    @Test
    void handler_with_audit_detail_and_missing_correlation_id_should_return_block_missing_correlation_id() {
        when(handlerMethod.getMethodAnnotation(AuditExclude.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(AuditDetail.class)).thenReturn(mock(AuditDetail.class));
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn(null);

        assertThat(service.evaluate(handlerMethod, request)).isEqualTo(AuditDecision.BLOCK_MISSING_CORRELATION_ID);
    }

    @Test
    void handler_with_audit_detail_and_correlation_id_should_return_proceed_audited() {
        final AuditDetail detail = mock(AuditDetail.class);
        when(detail.origin()).thenReturn("my-service");
        when(detail.eventName()).thenReturn("my-service.get-item");
        when(handlerMethod.getMethodAnnotation(AuditExclude.class)).thenReturn(null);
        when(handlerMethod.getMethodAnnotation(AuditDetail.class)).thenReturn(detail);
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("corr-001");

        assertThat(service.evaluate(handlerMethod, request)).isEqualTo(AuditDecision.PROCEED_AUDITED);
    }

    private AuditExclude auditExcludeInstance() {
        return new AuditExclude() {
            @Override public Class<AuditExclude> annotationType() { return AuditExclude.class; }
        };
    }
}
