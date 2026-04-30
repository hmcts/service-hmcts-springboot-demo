package uk.gov.hmcts.marketplace.postgres.encrypt.encryption;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.AllArgsConstructor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.stereotype.Component;

/**
 * Registers EncryptionEventListener with Hibernate's global event system so that
 * @Encrypted field processing happens for every entity without any per-entity
 * configuration.
 */
@Component
@AllArgsConstructor
public class HibernateListenerRegistrar {

    private final EntityManagerFactory entityManagerFactory;
    private final EncryptionEventListener encryptionEventListener;

    @PostConstruct
    public void registerListeners() {
        SessionFactoryImplementor sfi = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sfi.getServiceRegistry().getService(EventListenerRegistry.class);

        registry.prependListeners(EventType.PRE_INSERT, encryptionEventListener);
        registry.prependListeners(EventType.PRE_UPDATE, encryptionEventListener);
        registry.prependListeners(EventType.POST_LOAD, encryptionEventListener);
    }
}
