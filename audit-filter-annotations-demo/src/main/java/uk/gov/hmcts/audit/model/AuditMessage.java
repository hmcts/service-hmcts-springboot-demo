package uk.gov.hmcts.audit.model;

/**
 * Top-level audit message envelope — matches the CP Audit Message Format spec.
 * See: https://tools.hmcts.net/confluence/spaces/CPPGM/pages/1899790847/Audit
 */
public record AuditMessage(
    String origin,
    String component,
    String timestamp,
    AuditPayload content
) {}
