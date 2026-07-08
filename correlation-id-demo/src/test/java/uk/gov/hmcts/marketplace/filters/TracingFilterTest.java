package uk.gov.hmcts.marketplace.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TracingFilterTest {

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;
    @Mock UUIDService uuidService;

    @InjectMocks
    TracingFilter tracingFilter;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void filter_should_skip_root_and_actuator_paths() {
        when(request.getRequestURI()).thenReturn("/");
        assertThat(tracingFilter.shouldNotFilter(request)).isTrue();

        when(request.getRequestURI()).thenReturn("/actuator/health");
        assertThat(tracingFilter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void filter_should_apply_to_all_other_paths() {
        when(request.getRequestURI()).thenReturn("/api/something");
        assertThat(tracingFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void request_with_correlation_id_header_should_use_it_in_mdc_and_response() throws ServletException, IOException {
        final String correlationId = UUID.randomUUID().toString();
        when(request.getHeader(TracingFilter.CORRELATION_ID_KEY)).thenReturn(correlationId);

        tracingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(TracingFilter.CORRELATION_ID_KEY, correlationId);
        verify(filterChain).doFilter(request, response);
        assertThat(MDC.get(TracingFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void request_without_correlation_id_header_should_generate_one() throws ServletException, IOException {
        final String generated = UUID.randomUUID().toString();
        when(uuidService.randomString()).thenReturn(generated);
        when(request.getHeader(TracingFilter.CORRELATION_ID_KEY)).thenReturn(null);

        tracingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(TracingFilter.CORRELATION_ID_KEY, generated);
        verify(filterChain).doFilter(request, response);
    }
}
