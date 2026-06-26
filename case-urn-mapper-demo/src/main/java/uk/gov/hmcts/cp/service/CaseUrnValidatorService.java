package uk.gov.hmcts.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class CaseUrnValidatorService {

    static final String CASE_URN_REGEX = "^[0-9a-zA-Z]{1,30}$";

    public void validate(String caseUrn) {
        if (caseUrn == null || !caseUrn.matches(CASE_URN_REGEX)) {
            log.info("CaseUrn {} does not match expected regex:{}", Encode.forJava(caseUrn), CASE_URN_REGEX);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case URN must be 1-30 alphanumeric characters");
        }
    }
}
