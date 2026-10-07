package uk.gov.hmcts.amp.registration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.amp.registration.apim.ApimClient;
import uk.gov.hmcts.amp.registration.config.RegistrationProperties;
import uk.gov.hmcts.amp.registration.entra.EntraGraphClient;
import uk.gov.hmcts.amp.registration.service.TokenClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** The requests to Graph and Azure, checked against a stand-in server: what is sent, to where, in what order. */
class ClientsTest {

    private static final String ARM = "http://arm/subscriptions/sub-1/resourceGroups/rg-1/providers/"
        + "Microsoft.ApiManagement/service/apim-1/subscriptions/";

    private MockRestServiceServer server;
    private EntraGraphClient entra;
    private ApimClient apim;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient http = builder.build();
        RegistrationProperties properties = new RegistrationProperties(
            new RegistrationProperties.Entra("http://entra/token", "http://graph", "onboarding", "o-secret"),
            new RegistrationProperties.Apim("http://arm-entra/token", "http://arm", "apim-id", "a-secret",
                "sub-1", "rg-1", "apim-1", Map.of()));
        TokenClient tokens = new TokenClient(http);
        entra = new EntraGraphClient(http, tokens, properties, java.time.Duration.ZERO);
        apim = new ApimClient(http, tokens, properties);
    }

    private void expectToken(final String url) {
        server.expect(requestTo(url)).andExpect(method(HttpMethod.POST))
            .andExpect(content().string(containsString("grant_type=client_credentials")))
            .andRespond(withSuccess("{\"access_token\":\"the-token\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    void registering_should_create_the_application_then_its_service_principal_then_a_secret() {
        expectToken("http://entra/token");
        server.expect(requestTo("http://graph/applications")).andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer the-token"))
            .andExpect(content().json("{\"displayName\":\"My App\"}"))
            .andRespond(withSuccess("{\"id\":\"object-1\",\"appId\":\"client-1\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://graph/servicePrincipals")).andExpect(method(HttpMethod.POST))
            .andExpect(content().json("{\"appId\":\"client-1\"}"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://graph/applications/object-1/addPassword"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{\"keyId\":\"key-1\",\"secretText\":\"s3cret\"}", MediaType.APPLICATION_JSON));

        EntraGraphClient.Registration registration = entra.register("My App");

        assertThat(registration.clientId()).isEqualTo("client-1");
        assertThat(registration.clientSecret()).isEqualTo("s3cret");
        assertThat(registration.keyId()).isEqualTo("key-1");
        server.verify();
    }

    @Test
    void calls_graph_refuses_just_after_creating_an_application_should_be_retried_as_the_real_graph_needs() {
        expectToken("http://entra/token");
        server.expect(requestTo("http://graph/applications"))
            .andRespond(withSuccess("{\"id\":\"object-1\",\"appId\":\"client-1\"}", MediaType.APPLICATION_JSON));
        // What the real Graph answers a moment after the application exists: a 403 for the service principal...
        server.expect(requestTo("http://graph/servicePrincipals")).andRespond(withForbiddenRequest());
        server.expect(requestTo("http://graph/servicePrincipals"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        // ...and a 4xx for the secret.
        server.expect(requestTo("http://graph/applications/object-1/addPassword")).andRespond(withResourceNotFound());
        server.expect(requestTo("http://graph/applications/object-1/addPassword"))
            .andRespond(withSuccess("{\"keyId\":\"key-1\",\"secretText\":\"s3cret\"}", MediaType.APPLICATION_JSON));

        assertThat(entra.register("My App").clientSecret()).isEqualTo("s3cret");
        server.verify();
    }

    @Test
    void a_graph_call_that_keeps_being_refused_should_give_up_after_four_attempts() {
        expectToken("http://entra/token");
        server.expect(requestTo("http://graph/applications"))
            .andRespond(withSuccess("{\"id\":\"object-1\",\"appId\":\"client-1\"}", MediaType.APPLICATION_JSON));
        for (int attempt = 0; attempt < 4; attempt++) {
            server.expect(requestTo("http://graph/servicePrincipals")).andRespond(withForbiddenRequest());
        }

        assertThatThrownBy(() -> entra.register("My App")).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @Test
    void the_token_request_should_carry_the_onboarding_credentials() {
        server.expect(requestTo("http://entra/token"))
            .andExpect(content().string(containsString("client_id=onboarding")))
            .andExpect(content().string(containsString("client_secret=o-secret")))
            .andExpect(content().string(containsString("scope=https%3A%2F%2Fgraph.microsoft.com%2F.default")))
            .andRespond(withSuccess("{\"access_token\":\"the-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://graph/applications(appId='client-1')"))
            .andRespond(withNoContent());

        entra.delete("client-1");

        server.verify();
    }

    @Test
    void deleting_an_application_that_is_already_gone_should_not_be_a_failure() {
        expectToken("http://entra/token");
        server.expect(requestTo("http://graph/applications(appId='client-1')")).andExpect(method(HttpMethod.DELETE))
            .andRespond(withResourceNotFound());

        entra.delete("client-1");

        server.verify();
    }

    @Test
    void any_other_failure_deleting_an_application_should_surface() {
        expectToken("http://entra/token");
        server.expect(requestTo("http://graph/applications(appId='client-1')")).andRespond(withServerError());

        assertThatThrownBy(() -> entra.delete("client-1")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void a_token_response_with_no_token_should_be_refused() {
        server.expect(requestTo("http://entra/token")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> entra.register("My App")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void subscribing_should_put_a_subscription_scoped_to_the_product_and_return_its_key() {
        expectToken("http://arm-entra/token");
        server.expect(requestTo(startsWith(ARM))).andExpect(method(HttpMethod.PUT))
            .andExpect(requestTo(containsString("api-version=2022-08-01")))
            .andExpect(header("Authorization", "Bearer the-token"))
            .andExpect(content().json("{\"properties\":{\"scope\":\"/products/product-1\"}}"))
            .andRespond(withSuccess("{\"properties\":{\"primaryKey\":\"the-key\"}}", MediaType.APPLICATION_JSON));

        ApimClient.Subscription subscription = apim.subscribe("My App", "product-1");

        assertThat(subscription.subscriptionKey()).isEqualTo("the-key");
        assertThat(subscription.productId()).isEqualTo("product-1");
        assertThat(subscription.subscriptionName()).startsWith("my-app-");
        server.verify();
    }

    @Test
    void a_subscription_with_no_key_in_the_response_should_be_refused() {
        expectToken("http://arm-entra/token");
        server.expect(requestTo(startsWith(ARM))).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> apim.subscribe("My App", "product-1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unsubscribing_should_delete_the_subscription_by_name_and_tolerate_one_already_gone() {
        expectToken("http://arm-entra/token");
        server.expect(requestTo(ARM + "my-app-1a2b3c4d?api-version=2022-08-01"))
            .andExpect(method(HttpMethod.DELETE)).andRespond(withNoContent());
        expectToken("http://arm-entra/token");
        server.expect(requestTo(ARM + "my-app-1a2b3c4d?api-version=2022-08-01")).andRespond(withResourceNotFound());

        apim.unsubscribe("my-app-1a2b3c4d");
        apim.unsubscribe("my-app-1a2b3c4d");

        server.verify();
    }

    @Test
    void any_other_failure_unsubscribing_should_surface() {
        expectToken("http://arm-entra/token");
        server.expect(requestTo(startsWith(ARM))).andRespond(withServerError());

        assertThatThrownBy(() -> apim.unsubscribe("my-app-1a2b3c4d")).isInstanceOf(RuntimeException.class);
    }
}
