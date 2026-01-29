package uk.gov.hmcts.marketplace.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.marketplace.server.service.SubscriberStore;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecretStoreSeeder {

    public static final String DEFAULT_KEY_ID = "kid_v1";
    public static final String DEFAULT_SECRET = "demo-secret-please-change-in-production";

    @Bean
    CommandLineRunner seedDefaultKey(SubscriberStore subscriberStore) {
        return args -> {
            subscriberStore.putSecret(DEFAULT_KEY_ID, DEFAULT_SECRET);
            log.info("Seeded default keyId: {}", DEFAULT_KEY_ID);
        };
    }
}
