package uk.gov.hmcts.amp.entraapp.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "entra")
public record EntraProperties(String tenantId, String onboardingClientId,

        @NotBlank(message = "set ENTRA_ONBOARDING_CLIENT_SECRET - the amp-client-onboarding secret, not the "
                + "sign-in application's that entra-node-login-demo uses")
        String onboardingClientSecret,

        String graphBaseUrl, String loginBaseUrl) {
}
