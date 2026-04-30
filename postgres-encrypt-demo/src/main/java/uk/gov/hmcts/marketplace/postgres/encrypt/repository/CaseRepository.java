package uk.gov.hmcts.marketplace.postgres.encrypt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.marketplace.postgres.encrypt.domain.CaseEntity;

public interface CaseRepository extends JpaRepository<CaseEntity, Long> {
}
