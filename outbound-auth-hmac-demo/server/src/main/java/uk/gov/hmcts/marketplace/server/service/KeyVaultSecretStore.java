package uk.gov.hmcts.marketplace.server.service;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.security.keyvault.secrets.SecretClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeyVaultSecretStore {

    private final SecretClient secretClient;

    public void put(String keyId, String secret) {
        String name = toVaultName(keyId);
        log.info("Storing secret in Key Vault: name={}", name);
        secretClient.setSecret(name, secret);
        log.info("Secret stored in Key Vault: name={}", name);
    }

    public String get(String keyId) {
        String name = toVaultName(keyId);
        try {
            log.debug("Retrieving secret from Key Vault: name={}", name);
            String value = secretClient.getSecret(name).getValue();
            log.info("Secret retrieved from Key Vault: name={}", name);
            return value;
        } catch (ResourceNotFoundException e) {
            log.debug("Secret not found in Key Vault: name={}", name);
            return null;
        }
    }
    static String toVaultName(String keyId) {
        return keyId.replaceAll("[^a-zA-Z0-9-]", "-");
    }
}
