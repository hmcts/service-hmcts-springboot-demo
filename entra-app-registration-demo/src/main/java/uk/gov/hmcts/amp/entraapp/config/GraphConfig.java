package uk.gov.hmcts.amp.entraapp.config;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphConfig {

    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";

    @Bean
    public GraphServiceClient graphServiceClient(final EntraProperties config) {
        return new GraphServiceClient(new ClientSecretCredentialBuilder()
                .tenantId(config.tenantId())
                .clientId(config.onboardingClientId())
                .clientSecret(config.onboardingClientSecret())
                .build(), GRAPH_SCOPE);
    }
}
