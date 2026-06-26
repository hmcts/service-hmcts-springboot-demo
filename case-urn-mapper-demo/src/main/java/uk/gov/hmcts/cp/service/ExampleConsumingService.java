package uk.gov.hmcts.cp.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;

@Service
@Slf4j
@AllArgsConstructor
public class ExampleConsumingService {

    CaseUrnMapperService caseUrnMapperService;

    public CaseMapperResponse getCaseId(String caseUrn) {
        return caseUrnMapperService.getCaseId(caseUrn);
    }
}
