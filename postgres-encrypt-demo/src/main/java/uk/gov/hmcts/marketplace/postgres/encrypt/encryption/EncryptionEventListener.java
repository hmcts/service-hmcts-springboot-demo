package uk.gov.hmcts.marketplace.postgres.encrypt.encryption;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.marketplace.postgres.encrypt.annotation.Encrypted;

import java.lang.reflect.Field;

/**
 * Hibernate event listener that intercepts entity lifecycle events to transparently
 * encrypt @Encrypted fields before they reach the database and decrypt them on load.
 *
 * No entity or repository code needs to know about encryption.
 */
@Component
@AllArgsConstructor
@Slf4j
public class EncryptionEventListener implements PreInsertEventListener, PreUpdateEventListener, PostLoadEventListener {

    private final EncryptionService encryptionService;

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        encryptState(event.getEntity(), event.getState(), event.getPersister().getPropertyNames());
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        encryptState(event.getEntity(), event.getState(), event.getPersister().getPropertyNames());
        return false;
    }

    @Override
    public void onPostLoad(PostLoadEvent event) {
        decryptEntityFields(event.getEntity());
    }

    /**
     * Encrypts @Encrypted String values in the Hibernate state array (the values
     * that will be written to the DB). The in-memory entity field is left unchanged
     * so the caller always sees plain text.
     */
    private void encryptState(Object entity, Object[] state, String[] propertyNames) {
        for (int i = 0; i < propertyNames.length; i++) {
            if (!(state[i] instanceof String value)) {
                continue;
            }
            try {
                Field field = entity.getClass().getDeclaredField(propertyNames[i]);
                if (field.isAnnotationPresent(Encrypted.class)) {
                    log.debug("Encrypting field '{}' on {}", field.getName(), entity.getClass().getSimpleName());
                    state[i] = encryptionService.encrypt(value);
                }
            } catch (NoSuchFieldException ignored) {
                // property name may not match field name (e.g. mapped superclass) — skip
            }
        }
    }

    /**
     * Decrypts @Encrypted String fields directly on the entity after Hibernate
     * populates it from the database.
     */
    private void decryptEntityFields(Object entity) {
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Encrypted.class)) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value instanceof String encrypted) {
                    log.debug("Decrypting field '{}' on {}", field.getName(), entity.getClass().getSimpleName());
                    field.set(entity, encryptionService.decrypt(encrypted));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to decrypt field: " + field.getName(), e);
            }
        }
    }
}
