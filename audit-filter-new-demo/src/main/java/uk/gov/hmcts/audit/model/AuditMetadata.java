package uk.gov.hmcts.audit.model;

import java.util.UUID;

/**
 * {@code content._metadata} — mandatory per CP Audit Message Format spec.
 */
public record AuditMetadata(
    UUID id,
    String name,
    AuditContext context
) {}
