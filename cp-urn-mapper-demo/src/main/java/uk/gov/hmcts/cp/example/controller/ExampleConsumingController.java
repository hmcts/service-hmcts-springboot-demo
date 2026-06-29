package uk.gov.hmcts.cp.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.urnmapper.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnCertificateException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnNotFoundException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;
import uk.gov.hmcts.cp.example.service.ExampleConsumingService;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ExampleConsumingController {

    private final ExampleConsumingService exampleConsumingService;

    @GetMapping("/urnmapper/{caseUrn}")
    public ResponseEntity<CaseMapperResponse> getCaseIdByCaseUrn(@PathVariable String caseUrn) throws CaseUrnNotFoundException, CaseUrnValidationException, CaseUrnCertificateException {
        CaseMapperResponse response = exampleConsumingService.getCaseId(caseUrn);
        log.info("Mapped caseUrn:{} to caseId:{}", response.getCaseUrn(), response.getCaseId());
        return ResponseEntity.ok(response);
    }
}
