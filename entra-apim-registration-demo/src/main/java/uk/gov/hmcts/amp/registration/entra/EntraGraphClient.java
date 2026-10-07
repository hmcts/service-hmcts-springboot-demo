package uk.gov.hmcts.amp.registration.entra;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uk.gov.hmcts.amp.registration.config.RegistrationProperties;
import uk.gov.hmcts.amp.registration.service.TokenClient;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registers an application with Microsoft Entra through Microsoft Graph, and takes it away again: create the
 * application (which gives its Client ID), create its service principal, add a client secret (the only time
 * the secret is ever visible). The same calls the real marketplace service makes.
 *
 * <p>The last two are retried. Run against the real Graph this was not optional: straight after an application
 * is created, Graph refuses the next calls about it for a few seconds - the service principal with a 403
 * ("the backing application ... must in the local tenant"), the secret with a 4xx - because the directory has
 * not caught up with the write. A stand-in never does that, which is why the demo needed telling by the real thing.
 */
@Component
public class EntraGraphClient {

    private static final int ATTEMPTS = 4;

    public record Registration(String clientId, String clientSecret, String keyId) {
    }

    private record Created(String id, String appId) {
    }

    private record Password(String keyId, String secretText) {
    }

    private final RestClient http;
    private final TokenClient tokens;
    private final RegistrationProperties.Entra config;
    private final Duration retryDelay;

    @Autowired
    public EntraGraphClient(final RestClient http, final TokenClient tokens, final RegistrationProperties properties) {
        this(http, tokens, properties, Duration.ofSeconds(2));
    }

    // For a test, which should not wait two seconds between attempts.
    public EntraGraphClient(final RestClient http, final TokenClient tokens, final RegistrationProperties properties,
                            final Duration retryDelay) {
        this.http = http;
        this.tokens = tokens;
        this.config = properties.entra();
        this.retryDelay = retryDelay;
    }

    public Registration register(final String applicationName) {
        String token = token();
        Created application = post(token, "/applications", Map.of("displayName", applicationName), Created.class);
        withRetry(() -> post(token, "/servicePrincipals", Map.of("appId", application.appId()), Map.class));
        Password password = withRetry(() -> post(token, "/applications/" + application.id() + "/addPassword",
            Map.of("passwordCredential", Map.of("displayName", "demo-generated")), Password.class));
        return new Registration(application.appId(), password.secretText(), password.keyId());
    }

    /** Deletes the application by Client ID (and with it its service principal). Already gone is fine. */
    public void delete(final String clientId) {
        try {
            http.delete().uri(config.graphBaseUrl() + "/applications(appId='" + clientId + "')")
                .header("Authorization", "Bearer " + token()).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
        }
    }

    private <T> T withRetry(final Supplier<T> call) {
        RestClientException last = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (RestClientException e) {
                last = e;
                if (attempt < ATTEMPTS) {
                    pause();
                }
            }
        }
        throw last;
    }

    private void pause() {
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry a Graph call", e);
        }
    }

    private String token() {
        return tokens.fetch(config.tokenUrl(), config.clientId(), config.clientSecret(),
            "https://graph.microsoft.com/.default");
    }

    private <T> T post(final String token, final String path, final Object body, final Class<T> type) {
        return http.post().uri(config.graphBaseUrl() + path).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(type);
    }
}
