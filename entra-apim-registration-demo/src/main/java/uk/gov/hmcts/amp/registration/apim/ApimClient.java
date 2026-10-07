package uk.gov.hmcts.amp.registration.apim;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.amp.registration.config.RegistrationProperties;
import uk.gov.hmcts.amp.registration.service.TokenClient;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Issues an Azure API Management Subscription Key for one API (an APIM Product), and takes it away again. One
 * PUT to Azure Resource Manager creates the subscription and returns its key in the same response.
 */
@Component
public class ApimClient {

    public record Subscription(String productId, String subscriptionName, String subscriptionKey) {
    }

    private record Created(Properties properties) {
    }

    private record Properties(String primaryKey) {
    }

    private final RestClient http;
    private final TokenClient tokens;
    private final RegistrationProperties.Apim config;

    public ApimClient(final RestClient http, final TokenClient tokens, final RegistrationProperties properties) {
        this.http = http;
        this.tokens = tokens;
        this.config = properties.apim();
    }

    public Subscription subscribe(final String applicationName, final String productId) {
        String name = nameFor(applicationName);
        Created created = http.put().uri(subscriptionUrl(name)).header("Authorization", "Bearer " + token())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("properties", Map.of(
                "scope", "/products/" + productId,
                "displayName", applicationName + " (" + productId + ")")))
            .retrieve().body(Created.class);
        if (created == null || created.properties() == null || created.properties().primaryKey() == null) {
            throw new IllegalStateException("APIM returned no subscription key");
        }
        return new Subscription(productId, name, created.properties().primaryKey());
    }

    /** Deletes a subscription, and with it its key. Already gone is fine. */
    public void unsubscribe(final String subscriptionName) {
        try {
            http.delete().uri(subscriptionUrl(subscriptionName)).header("Authorization", "Bearer " + token())
                .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
        }
    }

    private String token() {
        return tokens.fetch(config.tokenUrl(), config.clientId(), config.clientSecret(),
            "https://management.azure.com/.default");
    }

    // Short and unique per call, so subscribing again after a delete never meets the old, revoked name.
    private String nameFor(final String applicationName) {
        String slug = applicationName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return slug + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String subscriptionUrl(final String subscriptionName) {
        return config.armBaseUrl() + "/subscriptions/" + config.subscriptionId() + "/resourceGroups/"
            + config.resourceGroup() + "/providers/Microsoft.ApiManagement/service/" + config.serviceName()
            + "/subscriptions/" + subscriptionName + "?api-version=2022-08-01";
    }
}
