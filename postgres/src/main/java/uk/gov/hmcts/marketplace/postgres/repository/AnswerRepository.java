package uk.gov.hmcts.marketplace.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {

    long countDistinctByCaseId(UUID caseId);

    // Sometimes using jpa query methods can be more complex than a simple query so we
    // can still query using the jpa objects but preferably NOT native queries
    @Query("select a from AnswerEntity a " +
            "where a.caseId=:caseId")
    List<AnswerEntity> queryExample(UUID caseId);
}
