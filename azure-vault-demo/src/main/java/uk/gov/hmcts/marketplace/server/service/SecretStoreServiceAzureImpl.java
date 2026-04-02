package uk.gov.hmcts.cp.vault;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class SecretStoreServiceAzureImpl implements SecretStoreServiceInterface {

    private static final String SECRET_PREFIX = "amp.subscription";

    private SecretClient secretClient;

    public SecretStoreServiceAzureImpl(final VaultServiceProperties vaultServiceProperties) {
        if (vaultServiceProperties.isVaultEnabled()) {
            final DefaultAzureCredential credential = new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(vaultServiceProperties.getVaultClientId().toString())
                    .build();
            this.secretClient = new SecretClientBuilder()
                    .vaultUrl(vaultServiceProperties.getVaultUri())
                    .credential(credential)
                    .buildClient();
        }
    }

    public Optional<String> getSecret(final String secretName) {
        final String fullName = String.format("%s.%s", SECRET_PREFIX, secretName);
        try {
            final KeyVaultSecret secret = secretClient.getSecret(fullName);
            return Optional.of(secret.getValue());
        } catch (ResourceNotFoundException e) {
            log.error("Secret not found");
            return Optional.empty();
        }
    }

    public void setSecret(final String secretName, final String secretValue) {
        final String fullName = String.format("%s.%s", SECRET_PREFIX, secretName);
        secretClient.setSecret(new KeyVaultSecret(fullName, secretValue));
    }
}
