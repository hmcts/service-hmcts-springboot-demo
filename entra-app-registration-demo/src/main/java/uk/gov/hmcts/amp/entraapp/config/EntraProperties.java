package uk.gov.hmcts.amp.entraapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which tenant to register applications in, and the onboarding identity to do it as.
 * {@code graphBaseUrl} and {@code loginBaseUrl} are settings so a stand-in can take Microsoft's place.
 */
@ConfigurationProperties(prefix = "entra")
public record EntraProperties(String tenantId, String onboardingClientId, String onboardingClientSecret,
                              String graphBaseUrl, String loginBaseUrl) {
}
