package uk.gov.hmcts.audit.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import uk.gov.hmcts.audit.model.AuditEventType;
import uk.gov.hmcts.audit.model.AuditMdcKeys;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditDecisionService auditDecisionService;
    private final AuditPayloadGenerationService auditPayloadGenerationService;

    public void process(final HandlerMethod handlerMethod, final HttpServletRequest request,
                        final HttpServletResponse response, final FilterChain chain)
            throws ServletException, IOException {

        switch (auditDecisionService.evaluate(handlerMethod, request)) {
            case PROCEED_EXCLUDED -> chain.doFilter(request, response);
            case PROCEED_AUDITED  -> audit(handlerMethod, request, response, chain);
            default               -> block(response);
        }
    }

    private void audit(final HandlerMethod handlerMethod, final HttpServletRequest request,
                       final HttpServletResponse response, final FilterChain chain)
            throws ServletException, IOException {
        try {
            auditRequest(handlerMethod, request);
        } catch (Exception e) {
            log.error("[AUDIT] Request audit failed, blocking — {}", e.getMessage());
            block(response);
            return;
        }
        chain.doFilter(request, response);
        auditResponse(handlerMethod, request, response.getStatus());
    }

    private void auditRequest(final HandlerMethod handlerMethod, final HttpServletRequest request) {
        final String payload = auditPayloadGenerationService.generate(handlerMethod, request, AuditEventType.REQUEST, null);
        log.info("[AUDIT] request payload={}", payload);
    }

    private void auditResponse(final HandlerMethod handlerMethod, final HttpServletRequest request, final int responseStatus) {
        final String payload = auditPayloadGenerationService.generate(handlerMethod, request, AuditEventType.RESPONSE, responseStatus);
        log.info("[AUDIT] response payload={}", payload);
        AuditMdcKeys.ALL.forEach(MDC::remove);
    }

    private void block(final HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().write("Audit required");
    }
}
