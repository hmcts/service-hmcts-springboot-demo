package uk.gov.hmcts.marketplace.postgres.encrypt.encryption;

/**
 * Marker interface for objects that can be stored as encrypted JSON in the database.
 * Implementing this interface enables the field to be annotated with
 * {@code @Convert(converter = JsonEncryptableConverter.class)}, which serialises the object
 * to JSON, encrypts it, and stores it as a single text column.
 * The implementing class must be Jackson-serialisable.
 */
public interface JsonEncryptable {
}
