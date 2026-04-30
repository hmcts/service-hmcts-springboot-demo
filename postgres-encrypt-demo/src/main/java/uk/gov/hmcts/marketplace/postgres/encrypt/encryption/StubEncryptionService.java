package uk.gov.hmcts.marketplace.postgres.encrypt.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * STUB implementation — wraps the value in XML-like tags to make encryption visible in the DB.
 * Replace this bean with an Azure Key Vault or AWS KMS implementation
 * without changing any entity or repository code.
 */
@Service
@Slf4j
public class StubEncryptionService implements EncryptionService {

    private static final String PREFIX = "<ENCRYPT>";
    private static final String SUFFIX = "</ENCRYPT>";

    @Override
    public String encrypt(String plainText) {
        log.debug("STUB encrypt called");
        return PREFIX + plainText + SUFFIX;
    }

    @Override
    public String decrypt(String cipherText) {
        log.debug("STUB decrypt called");
        return cipherText.substring(PREFIX.length(), cipherText.length() - SUFFIX.length());
    }
}
