package uk.gov.hmcts.cp.urnmapper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.openapi.ApiClient;
import uk.gov.hmcts.cp.openapi.api.CaseUrnMapperApi;

@Configuration
@Slf4j
public class CaseUrnMapperConfig {

    @Value("${case-urn-mapper-client.basepath}")
    String basePath;

    @Bean
    RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    CaseUrnMapperApi caseUrnMapperApi() {
        ApiClient apiClient = new ApiClient(restClient());
        apiClient.setBasePath(basePath);
        log.info("Initialised caseUrnMapperApi with basePath:{}", basePath);
        return new CaseUrnMapperApi(apiClient);
    }
}
