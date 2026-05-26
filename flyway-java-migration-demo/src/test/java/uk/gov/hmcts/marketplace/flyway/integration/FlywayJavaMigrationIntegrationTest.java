package uk.gov.hmcts.marketplace.flyway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.marketplace.flyway.config.TestContainersInitialise;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Flyway Java migration (V3__DeleteStaleTasks) runs at startup
 * and removes STALE tasks older than the threshold, while leaving others untouched.
 */
@SpringBootTest
@ContextConfiguration(initializers = TestContainersInitialise.class)
class FlywayJavaMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v3_java_migration_should_delete_stale_tasks_older_than_threshold() {
        // V2 seeds 2 STALE tasks (30 days and 60 days old) — both exceed the 7-day threshold
        Integer staleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task WHERE status = 'STALE'", Integer.class);
        assertThat(staleCount).isZero();
    }

    @Test
    void v3_java_migration_should_leave_non_stale_tasks_untouched() {
        // V2 seeds 2 PENDING + 1 COMPLETED — none should be deleted
        Integer remainingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task WHERE status != 'STALE'", Integer.class);
        assertThat(remainingCount).isEqualTo(3);
    }

    @Test
    void flyway_schema_history_should_record_java_migration_as_applied() {
        // V3 should appear in flyway_schema_history, confirming it is tracked like SQL migrations
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '3' AND success = true",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
