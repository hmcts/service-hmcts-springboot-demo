package marketplace.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditFilterTest {

    @Mock
    private MockHttpServletRequest request;
    @Mock
    private MockHttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    AuditFilter auditFilter;

    @Test
    void audit_filter_should_skip_if_header_is_set() {
        when(request.getHeader("skip-audit-filter")).thenReturn("true");
        assertThat(auditFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void audit_filter_should_log_details() throws ServletException, IOException {
        auditFilter.doFilterInternal(request,response, filterChain);
        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));
    }
}