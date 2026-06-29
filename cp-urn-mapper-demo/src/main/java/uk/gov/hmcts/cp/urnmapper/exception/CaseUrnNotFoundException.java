package uk.gov.hmcts.cp.urnmapper.exception;

public class CaseUrnNotFoundException extends Exception {

    public CaseUrnNotFoundException(String caseUrn) {
        super("No mapping found for caseUrn: " + caseUrn);
    }
}
