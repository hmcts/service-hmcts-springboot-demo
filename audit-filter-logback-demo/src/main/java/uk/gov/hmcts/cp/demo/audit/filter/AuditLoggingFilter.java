package uk.gov.hmcts.cp.demo.audit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditLoggingFilter extends OncePerRequestFilter {

    /**
     * Logger name must match the logger defined in logback-spring.xml for audit logs.
     */
    public static final String AUDIT_LOGGER_NAME = "AUDIT";

    private static final Logger auditLog = LoggerFactory.getLogger(AUDIT_LOGGER_NAME);

    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request, 1024 * 1024);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }

        ContentCachingRequestWrapper wrappedRequest = (ContentCachingRequestWrapper) request;
        ContentCachingResponseWrapper wrappedResponse = (ContentCachingResponseWrapper) response;

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
            logAudit(wrappedRequest, wrappedResponse);
        }
    }

    private void logAudit(ContentCachingRequestWrapper request,
                          ContentCachingResponseWrapper response) {
        try {
            Map<String, Object> audit = new HashMap<>();
            audit.put("loggerName", AUDIT_LOGGER_NAME);
            audit.put("timestamp", Instant.now().toString());
            audit.put("request", buildRequestPayload(request));
            audit.put("response", buildResponsePayload(response));
            String json = objectMapper.writeValueAsString(audit);
            auditLog.info(json);
        } catch (Exception e) {
            auditLog.warn("Failed to build audit log: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildRequestPayload(ContentCachingRequestWrapper request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("method", request.getMethod());
        payload.put("uri", request.getRequestURI());
        payload.put("queryString", request.getQueryString());
        payload.put("headers", headersFrom(request.getHeaderNames(), request::getHeader));
        String body = getCachedContent(request.getContentAsByteArray());
        if (body != null) {
            payload.put("body", body);
        }
        return payload;
    }

    private Map<String, Object> buildResponsePayload(ContentCachingResponseWrapper response) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", response.getStatus());
        payload.put("headers", response.getHeaderNames().stream()
            .collect(Collectors.toMap(name -> name, response::getHeader, (a, b) -> b)));
        String body = getCachedContent(response.getContentAsByteArray());
        if (body != null) {
            payload.put("body", body);
        }
        return payload;
    }

    private Map<String, String> headersFrom(Enumeration<String> names,
                                           java.util.function.Function<String, String> getter) {
        return Collections.list(names).stream()
            .collect(Collectors.toMap(name -> name, getter, (a, b) -> b));
    }

    private String getCachedContent(byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        String s = new String(content, StandardCharsets.UTF_8);
        return s.isBlank() ? null : s;
    }
}
