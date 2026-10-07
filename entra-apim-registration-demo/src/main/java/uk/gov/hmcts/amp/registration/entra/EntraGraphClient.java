package uk.gov.hmcts.amp.registration.entra;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.amp.registration.config.RegistrationProperties;
import uk.gov.hmcts.amp.registration.service.TokenClient;

import java.util.Map;

/**
 * Registers an application with Microsoft Entra through Microsoft Graph, and takes it away again: create the
 * application (which gives its Client ID), create its service principal, add a client secret (the only time
 * the secret is ever visible). The same calls the real marketplace service makes.
 */
@Component
public class EntraGraphClient {

    public record Registration(String clientId, String clientSecret, String keyId) {
    }

    private record Created(String id, String appId) {
    }

    private record Password(String keyId, String secretText) {
    }

    private final RestClient http;
    private final TokenClient tokens;
    private final RegistrationProperties.Entra config;

    public EntraGraphClient(final RestClient http, final TokenClient tokens, final RegistrationProperties properties) {
        this.http = http;
        this.tokens = tokens;
        this.config = properties.entra();
    }

    public Registration register(final String applicationName) {
        String token = token();
        Created application = post(token, "/applications", Map.of("displayName", applicationName), Created.class);
        post(token, "/servicePrincipals", Map.of("appId", application.appId()), Map.class);
        Password password = post(token, "/applications/" + application.id() + "/addPassword",
            Map.of("passwordCredential", Map.of("displayName", "demo-generated")), Password.class);
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

    private String token() {
        return tokens.fetch(config.tokenUrl(), config.clientId(), config.clientSecret(),
            "https://graph.microsoft.com/.default");
    }

    private <T> T post(final String token, final String path, final Object body, final Class<T> type) {
        return http.post().uri(config.graphBaseUrl() + path).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(type);
    }
}
