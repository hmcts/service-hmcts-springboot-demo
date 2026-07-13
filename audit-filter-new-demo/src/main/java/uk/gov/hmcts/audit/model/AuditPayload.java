package uk.gov.hmcts.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Map;
import java.util.UUID;

/**
 * {@code content} object within the CP audit envelope.
 * {@code _metadata}, {@code action}, and the domain ID fields are spec-defined.
 * {@code eventType}, {@code correlationId}, {@code responseStatus}, and {@code pathParams} are HMCTS filter extensions.
 * Domain ID fields are populated from MDC on the RESPONSE event — set by the service layer via {@link AuditMdcKeys}.
 */
@Builder
public record AuditPayload(
    @JsonProperty("_metadata") AuditMetadata metadata,
    AuditEventType eventType,
    String action,
    UUID materialId,
    UUID caseId,
    UUID hearingId,
    UUID courtDocumentId,
    String correlationId,
    Integer responseStatus,
    Map<String, UUID> pathParams
) {}
