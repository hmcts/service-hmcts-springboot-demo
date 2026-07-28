package uk.gov.hmcts.audit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.gov.hmcts.audit.service.AuditService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditFilterTest {

    @Mock private RequestMappingHandlerMapping handlerMapping;
    @Mock private AuditService auditService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain chain;
    @Mock private HandlerMethod handlerMethod;
    @Mock private HandlerExecutionChain executionChain;

    @InjectMocks
    private AuditFilter filter;

    @Test
    void resolved_handler_should_delegate_to_audit_service() throws Exception {
        when(handlerMapping.getHandler(request)).thenReturn(executionChain);
        when(executionChain.getHandler()).thenReturn(handlerMethod);

        filter.doFilterInternal(request, response, chain);

        verify(auditService).process(handlerMethod, request, response, chain);
    }

    @Test
    void unresolved_handler_should_pass_through_without_audit() throws Exception {
        when(handlerMapping.getHandler(request)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
