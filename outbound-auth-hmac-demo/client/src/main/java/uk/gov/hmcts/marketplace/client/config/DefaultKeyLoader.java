package uk.gov.hmcts.marketplace.client.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.marketplace.client.model.SecretResponse;
import uk.gov.hmcts.marketplace.client.service.SecretStore;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DefaultKeyLoader {

    public static final String DEFAULT_KEY_ID = "kid_v1";

    @Bean
    CommandLineRunner loadDefaultKeyIfServerAvailable(
            RestTemplate restTemplate,
            SecretStore secretStore,
            @Value("${client.server-base-url:http://localhost:8080}") String serverBaseUrl) {
        return args -> {
            try {
                SecretResponse response = restTemplate.getForEntity(
                        serverBaseUrl + "/api/secret?keyId=" + DEFAULT_KEY_ID,
                        SecretResponse.class
                ).getBody();
                if (response != null) {
                    secretStore.put(response.getKeyId(), response.getSecret());
                    log.info("Pre-loaded secret for default keyId: {}", response.getKeyId());
                }
            } catch (Exception e) {
                log.debug("Could not pre-load default key (server may not be up): {}", e.getMessage());
            }
        };
    }
}
