package uk.gov.hmcts.amp.entra.emulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * baseUrl and tenantId describe any Entra-compatible issuer: the emulator locally,
 * https://login.microsoftonline.com in a real tenant. adminBaseUrl has no real-Entra
 * equivalent — the emulator's directory management API lives there.
 */
@ConfigurationProperties(prefix = "entra")
public record EntraProperties(String baseUrl, String tenantId, String adminBaseUrl, boolean trustSelfSigned) {

    public String tokenEndpoint() {
        return "%s/%s/oauth2/v2.0/token".formatted(baseUrl, tenantId);
    }

    public String discoveryEndpoint() {
        return "%s/%s/v2.0/.well-known/openid-configuration".formatted(baseUrl, tenantId);
    }

    public String issuer() {
        return "%s/%s/v2.0".formatted(baseUrl, tenantId);
    }
}
