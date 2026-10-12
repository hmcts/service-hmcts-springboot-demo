package uk.gov.hmcts.amp.entraapp.config;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphConfig {

    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";

    /**
     * The onboarding identity acting as itself - a client credentials grant, not a signed-in user. The
     * SDK fetches and refreshes the token; nothing here handles one.
     *
     * <p>The secret is the only setting with no default, so it is the only one that can be missing.
     * Checked here so that it is a refusal to start saying what to do, rather than a 401 from Graph on
     * the first registration.
     */
    @Bean
    public GraphServiceClient graphServiceClient(final EntraProperties config) {
        if (config.onboardingClientSecret() == null || config.onboardingClientSecret().isBlank()) {
            throw new MissingOnboardingSecretException();
        }
        return new GraphServiceClient(new ClientSecretCredentialBuilder()
                .tenantId(config.tenantId())
                .clientId(config.onboardingClientId())
                .clientSecret(config.onboardingClientSecret())
                .build(), GRAPH_SCOPE);
    }
}
