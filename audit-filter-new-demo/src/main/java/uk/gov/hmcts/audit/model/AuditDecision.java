package uk.gov.hmcts.audit.model;

public enum AuditDecision {
    PROCEED_EXCLUDED,
    PROCEED_AUDITED,
    BLOCK_NO_ANNOTATION,
    BLOCK_MISSING_CORRELATION_ID
}
