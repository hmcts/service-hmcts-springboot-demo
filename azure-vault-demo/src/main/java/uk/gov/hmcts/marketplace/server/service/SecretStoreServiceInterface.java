package uk.gov.hmcts.cp.vault;

import java.util.Optional;

public interface SecretStoreServiceInterface {

    Optional<String> getSecret(String secretName);

    void setSecret(String secretName, String secretValue);
}
