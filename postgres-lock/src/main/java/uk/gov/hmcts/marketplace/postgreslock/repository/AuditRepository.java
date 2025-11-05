package uk.gov.hmcts.marketplace.postgreslock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.marketplace.postgreslock.entity.AuditEntity;

import java.util.Optional;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {

    @Query(
            value = "SELECT * FROM AUDIT a order by id limit 1 FOR UPDATE",
            nativeQuery = true)
    Optional<AuditEntity> findNextAudit();
}
