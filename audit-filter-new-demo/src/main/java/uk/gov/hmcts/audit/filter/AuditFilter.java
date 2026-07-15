package uk.gov.hmcts.audit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.gov.hmcts.audit.service.AuditService;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class AuditFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final RequestMappingHandlerMapping handlerMapping;
    private final AuditService auditService;

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain chain) throws ServletException, IOException {

        final HandlerMethod handlerMethod = resolveHandlerMethod(request);

        if (handlerMethod == null) {
            chain.doFilter(request, response);
            return;
        }

        auditService.process(handlerMethod, request, response, chain);
    }

    private HandlerMethod resolveHandlerMethod(final HttpServletRequest request) {
        try {
            final HandlerExecutionChain executionChain = handlerMapping.getHandler(request);
            if (executionChain != null && executionChain.getHandler() instanceof HandlerMethod method) {
                return method;
            }
        } catch (Exception e) {
            log.debug("[AUDIT] Could not resolve handler for {} {}", request.getMethod(), request.getRequestURI());
        }
        return null;
    }
}
