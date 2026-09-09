package uk.gov.hmcts.amp.entra.emulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccessToken(@JsonProperty("token_type") String tokenType,
                          @JsonProperty("expires_in") long expiresIn,
                          @JsonProperty("ext_expires_in") long extExpiresIn,
                          @JsonProperty("scope") String scope,
                          @JsonProperty("access_token") String accessToken) {
}
