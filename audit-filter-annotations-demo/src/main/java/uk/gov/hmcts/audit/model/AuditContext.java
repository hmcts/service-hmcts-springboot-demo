package uk.gov.hmcts.audit.model;

/**
 * {@code content._metadata.context} — {@code user} is the CP platform UUID of the authenticated user.
 * TODO: wire to Spring Security context to extract the real user UUID.
 */
public record AuditContext(
    String user
) {}
