package uk.gov.hmcts.cp.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.mappers.SubscriptionMapper;
import uk.gov.hmcts.cp.openapi.api.ExamplesApi;
import uk.gov.hmcts.cp.openapi.model.ExampleResponse;

@Service
@Slf4j
@AllArgsConstructor
public class OpenApiConsumerService {

    ExamplesApi examplesApi;
    SubscriptionMapper clientMapper;

    public AmpResponse getExample(long exampleId) {
        ExampleResponse r = examplesApi.getExampleByExampleId(exampleId);
        return clientMapper.mapClientResponse(r);
    }
}
