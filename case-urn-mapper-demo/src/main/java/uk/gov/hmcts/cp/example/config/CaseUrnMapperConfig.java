package uk.gov.hmcts.cp.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class CaseUrnMapperConfig {

    @Value("${case-urn-mapper-client.basepath}")
    String basePath;

    @Bean
    @ConditionalOnMissingBean(name = "caseUrnMapperRestClient")
    RestClient caseUrnMapperRestClient() {
        log.info("Initialised caseUrnMapperRestClient with basePath:{}", basePath);
        return RestClient.builder().baseUrl(basePath).build();
    }
}
