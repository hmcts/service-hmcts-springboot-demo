package uk.gov.hmcts.marketplace.flyway.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Flyway Java migration — runs exactly once at startup, tracked in flyway_schema_history.
 *
 * Annotating with @Component means Spring Boot automatically registers this as a
 * JavaMigration bean. Flyway picks it up from the Spring context, so full constructor
 * injection is available here just like any other Spring bean.
 *
 * Flyway's distributed lock ensures only one pod runs this even when multiple instances
 * start simultaneously.
 *
 * NOTE: We inject DataSource rather than the auto-configured JdbcTemplate or using Jpa beans.
 * Spring Boot's JdbcTemplate bean carries @DependsOn("flyway"), which would create
 * a circular dependency (Flyway → this bean → JdbcTemplate → Flyway).
 * DataSource has no such dependency, so we create a JdbcTemplate from it directly.
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class V3__Housekeeping extends BaseJavaMigration {

    private static final int STALE_THRESHOLD_DAYS = 7;

    private final DataSource dataSource;

    @Override
    public void migrate(final Context context) {
        log.info("Running startup maintenance: deleting STALE tasks older than {} days", STALE_THRESHOLD_DAYS);
        int deleted = new JdbcTemplate(dataSource).update(
            "DELETE FROM task WHERE status = 'STALE' AND created_at < now() - (? || ' days')::interval",
            STALE_THRESHOLD_DAYS
        );
        log.info("Startup maintenance complete: deleted {} stale tasks", deleted);
    }
}
