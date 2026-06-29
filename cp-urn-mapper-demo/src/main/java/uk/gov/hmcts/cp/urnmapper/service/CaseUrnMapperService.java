package uk.gov.hmcts.cp.urnmapper.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.hmcts.cp.urnmapper.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnCertificateException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnNotFoundException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;

import javax.net.ssl.SSLException;
import uk.gov.hmcts.cp.urnmapper.mapper.UrnMapperResponseMapper;
import uk.gov.hmcts.cp.openapi.api.CaseUrnMapperApi;

@Service
@Slf4j
@AllArgsConstructor
public class CaseUrnMapperService {

    CaseUrnMapperApi caseUrnMapperApi;
    UrnMapperResponseMapper mapper;
    CaseUrnValidatorService caseUrnValidatorService;

    public CaseMapperResponse getCaseId(String caseUrn) throws CaseUrnNotFoundException, CaseUrnValidationException, CaseUrnCertificateException {
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
        } catch (ResourceAccessException ex) {
            if (ex.getCause() instanceof SSLException) {
                throw new CaseUrnCertificateException(caseUrn, ex.getCause());
            }
            throw ex;
        }
    }
}
