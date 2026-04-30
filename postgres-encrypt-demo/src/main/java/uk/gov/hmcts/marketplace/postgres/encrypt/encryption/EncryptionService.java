package uk.gov.hmcts.marketplace.postgres.encrypt.encryption;

public interface EncryptionService {

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
