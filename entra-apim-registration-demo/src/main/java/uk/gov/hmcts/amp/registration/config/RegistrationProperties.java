package uk.gov.hmcts.amp.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Where Entra, Microsoft Graph and Azure API Management are, and what to call them with. The defaults in
 * application.yml point at the stand-ins in docker/docker-compose.yml; the real thing is the same
 * settings with Microsoft's addresses.
 */
@ConfigurationProperties(prefix = "registration")
public record RegistrationProperties(Entra entra, Apim apim) {

    public record Entra(String tokenUrl, String graphBaseUrl, String clientId, String clientSecret) {
    }

    public record Apim(String tokenUrl, String armBaseUrl, String clientId, String clientSecret,
                       String subscriptionId, String resourceGroup, String serviceName,
                       Map<String, String> products) {
    }
}
