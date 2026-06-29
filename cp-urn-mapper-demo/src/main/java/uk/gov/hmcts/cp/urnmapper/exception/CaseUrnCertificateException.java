package uk.gov.hmcts.cp.urnmapper.exception;

public class CaseUrnCertificateException extends Exception {

    public CaseUrnCertificateException(String caseUrn, Throwable cause) {
        super("SSL certificate error calling URN mapper for caseUrn: " + caseUrn, cause);
    }
}
