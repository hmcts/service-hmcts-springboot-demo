package uk.gov.hmcts.amp.registration.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Gets a client-credentials access token: how the app proves itself to Graph and to Azure Resource Manager. */
@Component
public class TokenClient {

    private record Token(@JsonProperty("access_token") String accessToken) {
    }

    private final RestClient http;

    public TokenClient(final RestClient http) {
        this.http = http;
    }

    public String fetch(final String tokenUrl, final String clientId, final String clientSecret, final String scope) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", scope);
        Token token = http.post().uri(tokenUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
            .retrieve().body(Token.class);
        if (token == null || token.accessToken() == null) {
            throw new IllegalStateException("The token endpoint returned no access token");
        }
        return token.accessToken();
    }
}
