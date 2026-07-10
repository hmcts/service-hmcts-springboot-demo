package uk.gov.hmcts.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import uk.gov.hmcts.audit.annotation.AuditDetail;

import org.slf4j.MDC;
import uk.gov.hmcts.audit.misc.service.ClockService;
import uk.gov.hmcts.audit.misc.service.UuidService;
import uk.gov.hmcts.audit.model.AuditContext;
import uk.gov.hmcts.audit.model.AuditEventType;
import uk.gov.hmcts.audit.model.AuditMdcKeys;
import uk.gov.hmcts.audit.model.AuditMessage;
import uk.gov.hmcts.audit.model.AuditMetadata;
import uk.gov.hmcts.audit.model.AuditPayload;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.hmcts.audit.filter.AuditFilter.CORRELATION_ID_HEADER;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditPayloadGenerationService {

    private final ObjectMapper objectMapper;
    private final ClockService clockService;
    private final UuidService uuidService;

    @Value("${material-client.cjscppuid}")
    private String systemUserId;

    public String generate(final HandlerMethod handlerMethod, final HttpServletRequest request,
                           final AuditEventType eventType, final Integer responseStatus) {
        final AuditDetail detail = resolveAnnotation(handlerMethod);
        if (detail == null) {
            log.error("[AUDIT] No @AuditDetail on handler — cannot generate payload for {} {}",
                    request.getMethod(), request.getRequestURI());
            return null;
        }
        try {
            final Map<String, UUID> pathParams = extractPathParams(detail, request);
            final AuditContext context   = new AuditContext(systemUserId);
            final AuditMetadata metadata = new AuditMetadata(uuidService.randomUUID(), detail.eventName(), context);
            final AuditPayload content = AuditPayload.builder()
                    .metadata(metadata)
                    .eventType(eventType)
                    .action(detail.action())
                    .materialId(resolveFromMdc(AuditMdcKeys.MATERIAL_ID))
                    .caseId(resolveFromMdc(AuditMdcKeys.CASE_ID))
                    .hearingId(resolveFromMdc(AuditMdcKeys.HEARING_ID))
                    .courtDocumentId(resolveFromMdc(AuditMdcKeys.COURT_DOCUMENT_ID))
                    .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                    .responseStatus(responseStatus)
                    .pathParams(pathParams)
                    .build();
            final AuditMessage payload = new AuditMessage(
                    detail.origin(),
                    detail.component(),
                    clockService.now().toString(),
                    content);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("[AUDIT] Failed to generate audit payload for {} {}",
                    request.getMethod(), request.getRequestURI(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, UUID> extractPathParams(final AuditDetail detail, final HttpServletRequest request) {
        if (detail.pathParams().length == 0) {
            return Map.of();
        }
        final Map<String, String> uriVariables = (Map<String, String>)
                request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVariables == null) {
            return Map.of();
        }
        return Arrays.stream(detail.pathParams())
                .filter(uriVariables::containsKey)
                .collect(Collectors.toMap(name -> name, name -> UUID.fromString(uriVariables.get(name))));
    }

    private AuditDetail resolveAnnotation(final HandlerMethod method) {
        final AuditDetail onMethod = method.getMethodAnnotation(AuditDetail.class);
        if (onMethod != null) {
            return onMethod;
        }
        return method.getBeanType().getAnnotation(AuditDetail.class);
    }



    private UUID resolveFromMdc(final String key) {
        final String value = MDC.get(key);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            log.warn("[AUDIT] MDC key {} is not a valid UUID: {}", key, value);
            return null;
        }
    }
}
