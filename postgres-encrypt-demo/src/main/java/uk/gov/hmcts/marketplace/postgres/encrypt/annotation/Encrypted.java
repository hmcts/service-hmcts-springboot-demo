package uk.gov.hmcts.marketplace.postgres.encrypt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String entity field for transparent encrypt-on-write / decrypt-on-read.
 * The field is stored encrypted in the database and decrypted automatically when loaded.
 * Swap the EncryptionService implementation to use Azure Key Vault or AWS KMS.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
}
