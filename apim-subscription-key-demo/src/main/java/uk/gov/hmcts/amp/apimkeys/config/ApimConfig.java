package uk.gov.hmcts.amp.apimkeys.config;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.apimanagement.ApiManagementManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApimConfig {

    /**
     * Uses DefaultAzureCredential which gets credentials from az login in local env or clientId and secret in real environment.
     * Tenant and subscription come from AZURE_TENANT_ID and AZURE_SUBSCRIPTION_ID, so neither GUID is in the repo.
     */
    @Bean
    public ApiManagementManager apiManagementManager(final ApimProperties config) {
        return ApiManagementManager.authenticate(
                new DefaultAzureCredentialBuilder().build(),
                new AzureProfile(config.tenantId(), config.subscriptionId(), AzureEnvironment.AZURE));
    }
}
