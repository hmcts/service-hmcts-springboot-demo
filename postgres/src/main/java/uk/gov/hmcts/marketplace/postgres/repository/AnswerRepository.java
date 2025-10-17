package uk.gov.hmcts.marketplace.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {

    List<AnswerEntity> findByCaseId(UUID queryId);

    Optional<AnswerEntity> findFirstByQueryIdOrderByCreatedAtDesc(UUID queryId);

    long countDistinctByQueryId(UUID queryId);

    // Sometimes using jpa query methods can be more complex than a simple query so we
    // can still query using the jpa objects and preferably not native queries
    @Query("select a from AnswerNewEntity a " +
            "where a.caseId=:caseId and a.queryId=:queryId and a.createdAt<= :before")
    List<AnswerEntity> findLatestBeforeGivenTime(UUID caseId, UUID queryId, OffsetDateTime before);
}
