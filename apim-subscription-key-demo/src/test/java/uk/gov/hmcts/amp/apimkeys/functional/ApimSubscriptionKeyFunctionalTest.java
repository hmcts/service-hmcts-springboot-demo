package uk.gov.hmcts.amp.apimkeys.functional;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.amp.apimkeys.config.ApimProperties;
import uk.gov.hmcts.amp.apimkeys.service.ApimSubscriptionKeyService;
import uk.gov.hmcts.amp.apimkeys.service.SubscriptionKey;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Operates against the real azure APIM, needs the real credentials through az login say
 */
@SpringBootTest
@Slf4j
@EnabledOnOs(OS.MAC)
class ApimSubscriptionKeyFunctionalTest {

    // This product must exist on the target instance
    private static final String PRODUCT_ID = "example-product";

    @Resource
    private ApimSubscriptionKeyService subscriptionService;

    @Resource
    private ApimProperties config;

    @Resource
    private ObjectMapper objectMapper;


    @Test
    void listing_subscriptions_should_list_to_stdout() {
        List<SubscriptionKey> listed = subscriptionService.list();
        log.info("Found {} subscriptions on {}", listed.size(), config.serviceName());
        log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listed));
        assertThat(listed).hasSizeGreaterThan(1);
    }

    @Test
    void creating_a_subscription_should_issue_a_key_and_deleting_it_should_take_it_away() {
        String name = "functest-subkey-" + UUID.randomUUID().toString().substring(0, 8);
        SubscriptionKey created = subscriptionService.create(name, PRODUCT_ID, "apim-subscription-key-demo");
        assertThat(created.primaryKey()).isNotBlank();

        SubscriptionKey read = subscriptionService.get(name);
        log.info("Created {} on {}", name, config.serviceName());

        assertThat(read.primaryKey()).isEqualTo(created.primaryKey());
        assertThat(read.scope()).endsWith("/products/" + PRODUCT_ID);

        subscriptionService.delete(name);

        assertThatThrownBy(() -> subscriptionService.get(name)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void read_first_subscription_key_in_list_should_have_primary_key() {
        List<SubscriptionKey> listed = subscriptionService.list();
        Assumptions.assumeFalse(listed.isEmpty(), "The instance has no subscriptions to read");

        SubscriptionKey found = subscriptionService.get(listed.getFirst().name());
        log.info("Subscription Key \"{}\" has primary key:{}", found.displayName(), found.primaryKey());
    }
}
