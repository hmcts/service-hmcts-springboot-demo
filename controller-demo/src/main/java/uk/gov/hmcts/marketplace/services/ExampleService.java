package uk.gov.hmcts.marketplace.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.ExampleResponse;

@Service
@Slf4j
public class ExampleService {

    public ExampleResponse getExampleById(final Long exampleId) {
        log.info("Fetching example for id: {}", exampleId);
        return ExampleResponse.builder()
            .exampleId(exampleId)
            .exampleText("Example response for id " + exampleId)
            .build();
    }
}
