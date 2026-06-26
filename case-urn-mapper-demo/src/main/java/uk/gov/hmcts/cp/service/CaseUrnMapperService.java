package uk.gov.hmcts.cp.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.exception.CaseUrnNotFoundException;
import uk.gov.hmcts.cp.mapper.UrnMapperResponseMapper;
import uk.gov.hmcts.cp.openapi.api.CaseUrnMapperApi;

@Service
@Slf4j
@AllArgsConstructor
public class CaseUrnMapperService {

    CaseUrnMapperApi caseUrnMapperApi;
    UrnMapperResponseMapper mapper;
    CaseUrnValidatorService caseUrnValidatorService;

    public CaseMapperResponse getCaseId(String caseUrn) {
        // Validation lives here rather than the controller so any future caller is protected
        caseUrnValidatorService.validate(caseUrn);
        log.info("Fetching caseId for caseUrn:{}", caseUrn);
        try {
            return mapper.toCaseMapperResponse(caseUrnMapperApi.getCaseIdByCaseUrn(caseUrn, false));
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new CaseUrnNotFoundException(caseUrn);
            }
            throw ex;
        }
    }
}
