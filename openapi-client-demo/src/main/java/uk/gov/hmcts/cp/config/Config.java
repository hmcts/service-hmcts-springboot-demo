package uk.gov.hmcts.cp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.openapi.ApiClient;
import uk.gov.hmcts.cp.openapi.api.ExamplesApi;

@Configuration
@Slf4j
public class Config {

    @Value("${example-client.basepath}")
    String basePath;

    @Bean
    RestClient restClient(){
        return RestClient.builder().build();
    }

    @Bean
    ExamplesApi examplesApi() {
        ApiClient apiClient = new ApiClient(restClient());
        apiClient.setBasePath(basePath);
        log.info("Initialised apiClient with basePath:{}", basePath);
        return new ExamplesApi(apiClient);
    }
}
