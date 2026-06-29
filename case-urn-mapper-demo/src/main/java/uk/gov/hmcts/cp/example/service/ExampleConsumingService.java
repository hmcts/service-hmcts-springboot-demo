package uk.gov.hmcts.cp.example.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.example.client.CaseUrnMapperClient;
import uk.gov.hmcts.cp.openapi.model.CaseMapperResponse;

import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class ExampleConsumingService {

    CaseUrnMapperClient caseUrnMapperClient;

    public UUID getCaseId(String caseUrn) {
        log.info("Fetching caseId for caseUrn:{}", caseUrn);
        CaseMapperResponse response = caseUrnMapperClient.getCaseMapping(caseUrn);
        return response.getCaseId();
    }
}
