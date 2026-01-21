package uk.gov.hmcts.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.ApiClient;
import uk.gov.hmcts.cp.openapi.api.ExamplesApi;
import uk.gov.hmcts.cp.openapi.model.ExampleResponse;

@Service
@Slf4j
public class OpenApiConsumerService {

    String basePath;
    ApiClient apiClient;

    public OpenApiConsumerService(@Value("${example-client.basepath}") String basePath, ApiClient apiClient) {
        log.info("OpenApiConsumer initialised with client url:{}", basePath);
        this.basePath = basePath;
        this.apiClient = apiClient;
    }

    public ExampleResponse getExample(long exampleId) {
        apiClient.setBasePath("http://localhost:8090");
        ExamplesApi examplesApi = new ExamplesApi(apiClient);
        return examplesApi.getExampleByExampleId(exampleId);
    }
}
