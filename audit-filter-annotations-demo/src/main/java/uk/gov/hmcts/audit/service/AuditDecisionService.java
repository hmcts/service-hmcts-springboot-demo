package uk.gov.hmcts.audit.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import uk.gov.hmcts.audit.annotation.AuditDetail;
import uk.gov.hmcts.audit.annotation.AuditExclude;
import uk.gov.hmcts.audit.model.AuditDecision;

import static uk.gov.hmcts.audit.filter.AuditFilter.CORRELATION_ID_HEADER;

@Component
@Slf4j
public class AuditDecisionService {

    public AuditDecision evaluate(final HandlerMethod handlerMethod, final HttpServletRequest request) {
        final AuditExclude exclude = resolveAnnotation(handlerMethod, AuditExclude.class);
        final AuditDetail detail   = resolveAnnotation(handlerMethod, AuditDetail.class);

        if (exclude != null) {
            log.debug("[AUDIT] {} {} — @AuditExclude, skipping", request.getMethod(), request.getRequestURI());
            return AuditDecision.PROCEED_EXCLUDED;
        }

        if (detail == null) {
            log.warn("[AUDIT] {} {} — no audit annotation, blocking", request.getMethod(), request.getRequestURI());
            return AuditDecision.BLOCK_NO_ANNOTATION;
        }

        final String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            log.warn("[AUDIT] {} {} — missing {}, blocking", request.getMethod(), request.getRequestURI(), CORRELATION_ID_HEADER);
            return AuditDecision.BLOCK_MISSING_CORRELATION_ID;
        }

        log.info("[AUDIT] {} {} correlationId={} origin={} event={}",
                request.getMethod(), request.getRequestURI(), correlationId, detail.origin(), detail.eventName());
        return AuditDecision.PROCEED_AUDITED;
    }

    private <A extends java.lang.annotation.Annotation> A resolveAnnotation(
            final HandlerMethod method, final Class<A> annotationType) {
        final A onMethod = method.getMethodAnnotation(annotationType);
        if (onMethod != null) {
            return onMethod;
        }
        return method.getBeanType().getAnnotation(annotationType);
    }
}
