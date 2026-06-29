package uk.gov.hmcts.cp.urnmapper.service;

import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;

@Service
@Slf4j
public class CaseUrnValidatorService {

    static final String CASE_URN_REGEX = "^[0-9a-zA-Z]{1,30}$";

    public void validate(String caseUrn) throws CaseUrnValidationException {
        if (caseUrn == null || !caseUrn.matches(CASE_URN_REGEX)) {
            log.info("CaseUrn {} does not match expected regex:{}", Encode.forJava(caseUrn), CASE_URN_REGEX);
            throw new CaseUrnValidationException("Case URN must be 1-30 alphanumeric characters");
        }
    }
}
