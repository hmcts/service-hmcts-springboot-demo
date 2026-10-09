package uk.gov.hmcts.amp.apimkeys.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.amp.apimkeys.config.WireMockApimInitialise;
import uk.gov.hmcts.amp.apimkeys.service.ApimSubscriptionKeyService;
import uk.gov.hmcts.amp.apimkeys.service.SubscriptionKey;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.amp.apimkeys.config.WireMockApimInitialise.INSTANCE;
import static uk.gov.hmcts.amp.apimkeys.config.WireMockApimInitialise.stubSubscriptionLifecycle;
import static uk.gov.hmcts.amp.apimkeys.config.WireMockApimInitialise.stubSubscriptionList;
import static uk.gov.hmcts.amp.apimkeys.config.WireMockApimInitialise.wireMockApim;

/**
 * The same journey as ApimSubscriptionKeyFunctionalTest, through the same Spring wiring, but with WireMock
 * where Azure API Management would be. No credential and no network beyond localhost, so unlike the functional
 * test this one runs in the pipeline.
 */
@SpringBootTest
@ContextConfiguration(initializers = WireMockApimInitialise.class)
@Slf4j
class ApimSubscriptionKeyIntegrationTest {

    private static final String PRODUCT_ID = "example-product";

    @Resource
    private ApimSubscriptionKeyService subscriptionService;

    @Resource
    private ObjectMapper objectMapper;

    @BeforeEach
    void forgetPreviousStubs() {
        wireMockApim().resetAll();
    }

    @Test
    void listing_subscriptions_should_list_to_stdout() {
        stubSubscriptionList();

        List<SubscriptionKey> listed = subscriptionService.list();

        log.info("Found {} subscriptions on the wireMockApim", listed.size());
        log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(listed));
        assertThat(listed).hasSizeGreaterThan(1);
    }

    @Test
    void creating_a_subscription_should_issue_a_key_and_deleting_it_should_take_it_away() {
        String name = "team-alpha";
        stubSubscriptionLifecycle(name);

        SubscriptionKey created = subscriptionService.create(name, PRODUCT_ID, "apim-subscription-key-demo");
        assertThat(created.primaryKey()).isNotBlank();

        SubscriptionKey read = subscriptionService.get(name);
        log.info("Created {} on the wireMock apim", name);

        assertThat(read.primaryKey()).isEqualTo(created.primaryKey());
        assertThat(read.scope()).endsWith("/products/" + PRODUCT_ID);

        subscriptionService.delete(name);

        assertThatThrownBy(() -> subscriptionService.get(name)).isInstanceOf(ResponseStatusException.class);

        // What the app actually put on the wire, rather than what the wiremock apim chose to answer.
        wireMockApim().verify(putRequestedFor(urlPathEqualTo(INSTANCE + "/subscriptions/" + name))
                .withRequestBody(equalToJson("""
                        {"properties":{"scope":"/products/example-product",
                         "displayName":"apim-subscription-key-demo","state":"active"}}""", true, true)));
        wireMockApim().verify(deleteRequestedFor(urlPathEqualTo(INSTANCE + "/subscriptions/" + name))
                .withHeader("If-Match", equalTo("*")));
    }
}
