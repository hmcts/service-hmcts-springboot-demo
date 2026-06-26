package uk.gov.hmcts.cp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.service.ExampleConsumingService;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ExampleConsumingController {

    private final ExampleConsumingService exampleConsumingService;

    @GetMapping("/urnmapper/{caseUrn}")
    public ResponseEntity<CaseMapperResponse> getCaseIdByCaseUrn(@PathVariable String caseUrn) {
        CaseMapperResponse response = exampleConsumingService.getCaseId(caseUrn);
        log.info("Mapped caseUrn:{} to caseId:{}", response.getCaseUrn(), response.getCaseId());
        return ResponseEntity.ok(response);
    }
}
