package uk.gov.hmcts.marketplace.flyway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.marketplace.flyway.domain.TaskEntity;

import java.time.Instant;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    int deleteByStatusAndCreatedAtBefore(String status, Instant before);
}
