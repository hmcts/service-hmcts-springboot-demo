package uk.gov.hmcts.cp.example.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.urnmapper.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnCertificateException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnNotFoundException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;
import uk.gov.hmcts.cp.urnmapper.service.CaseUrnMapperService;

@Service
@Slf4j
@AllArgsConstructor
public class ExampleConsumingService {

    CaseUrnMapperService caseUrnMapperService;

    public CaseMapperResponse getCaseId(String caseUrn) throws CaseUrnNotFoundException, CaseUrnValidationException, CaseUrnCertificateException {
        return caseUrnMapperService.getCaseId(caseUrn);
    }
}
