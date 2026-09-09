package uk.gov.hmcts.amp.entra.emulator.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.amp.entra.emulator.config.EntraProperties;
import uk.gov.hmcts.amp.entra.emulator.model.AccessToken;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * Posts the OAuth2 client credentials grant to {baseUrl}/{tenantId}/oauth2/v2.0/token.
 * The emulator and a real Entra tenant both answer this shape, so only configuration changes between them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntraTokenClient {

    private final RestClient entraRestClient;
    private final EntraProperties properties;

    public AccessToken clientCredentialsToken(final String clientId, final String clientSecret, final String resourceUri) {
        final String scope = resourceUri.endsWith("/.default") ? resourceUri : resourceUri + "/.default";
        log.info("Requesting client credentials token for client {} with scope {}", clientId, scope);
        return entraRestClient.post()
                .uri(properties.tokenEndpoint())
                .contentType(APPLICATION_FORM_URLENCODED)
                .body(form(clientId, clientSecret, scope))
                .retrieve()
                .body(AccessToken.class);
    }

    private static MultiValueMap<String, String> form(final String clientId, final String clientSecret, final String scope) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", scope);
        return form;
    }
}
