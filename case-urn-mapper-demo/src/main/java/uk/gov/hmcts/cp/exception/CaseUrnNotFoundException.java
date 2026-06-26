package uk.gov.hmcts.cp.exception;

public class CaseUrnNotFoundException extends RuntimeException {

    public CaseUrnNotFoundException(String caseUrn) {
        super("No mapping found for caseUrn: " + caseUrn);
    }
}
