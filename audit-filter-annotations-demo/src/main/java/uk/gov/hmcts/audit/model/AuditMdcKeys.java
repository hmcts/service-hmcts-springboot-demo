package uk.gov.hmcts.audit.model;

import java.util.List;

public final class AuditMdcKeys {

    public static final String MATERIAL_ID       = "audit.materialId";
    public static final String CASE_ID           = "audit.caseId";
    public static final String HEARING_ID        = "audit.hearingId";
    public static final String COURT_DOCUMENT_ID = "audit.courtDocumentId";

    public static final List<String> ALL = List.of(MATERIAL_ID, CASE_ID, HEARING_ID, COURT_DOCUMENT_ID);

    private AuditMdcKeys() {}
}
