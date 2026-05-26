# flyway-java-migration-demo

Demonstrates using **Flyway Java migrations** to run one-off maintenance jobs at application startup — with full Spring dependency injection, run-once guarantees, and distributed locking built in.

## The pattern

```
V1__create_task_table.sql   ← normal SQL migration
V2__seed_tasks.sql          ← normal SQL migration
V3__Housekeeping.java   ← Java migration: Spring bean, runs maintenance logic
```

Flyway tracks all three in `flyway_schema_history` — SQL and Java migrations are treated identically.

## Why Java migrations for maintenance jobs?

| Concern | How Flyway handles it |
|---|---|
| Run exactly once | Tracked in `flyway_schema_history` — never reruns |
| Distributed lock | Flyway acquires a DB lock — only one pod runs it even if ten start simultaneously |
| Spring injection | `@Component` on the migration class means Spring wires it up like any other bean |
| Cleanup | Delete the class when done; add a placeholder SQL so the version number isn't reused |

## Key implementation detail — avoiding the circular dependency

Spring Boot's auto-configured `JdbcTemplate` has `@DependsOn("flyway")` to guarantee migrations
run before the template is used. If a Java migration bean injects that `JdbcTemplate`, you get a cycle:

```
Flyway → V3 migration → service → JdbcTemplate → (waits for) Flyway  ← deadlock
```

The fix: inject `DataSource` instead. It has no Flyway dependency, so you create a `JdbcTemplate`
from it directly inside the service:

```java
// ✅ DataSource has no @DependsOn("flyway") — safe to inject here
private final DataSource dataSource;

public int deleteStaleTasksOlderThanDays(final int days) {
    return new JdbcTemplate(dataSource).update("DELETE FROM task WHERE ...", days);
}
```

## The migration class

```java
@Component                          // Spring Boot auto-registers as a JavaMigration bean
@RequiredArgsConstructor
public class V3__Housekeeping extends BaseJavaMigration {

    private final DataSource dataSource;  // inject DataSource, not JdbcTemplate (see below)

    @Override
    public void migrate(final Context context) {
        int deleted = new JdbcTemplate(dataSource).update(
            "DELETE FROM task WHERE status = 'STALE' AND created_at < now() - (? || ' days')::interval",
            7
        );
        log.info("Deleted {} stale tasks", deleted);
    }
}
```

No `@RequestMapping`, no scheduler, no `@EventListener` — Flyway calls `migrate()` at startup,
once, with the distributed lock held.

## Structure

```
flyway-java-migration-demo/
└── src/main/
    ├── java/uk/gov/hmcts/marketplace/flyway/
    │   ├── Application.java
    │   └── migration/
    │       └── V3__DeleteStaleTasks.java   ← @Component extending BaseJavaMigration, injects DataSource
    └── resources/
        └── db/migration/
            ├── V1__create_task_table.sql
            └── V2__seed_tasks.sql
```

## Running locally

Requires a running Postgres instance (see `postgres-springboot4/docker/docker-compose.yml`).

```bash
./gradlew bootRun
```

## Running tests

Uses Testcontainers — no local Postgres needed.

```bash
./gradlew test
```

Tests verify:
- STALE tasks older than the threshold are deleted by V3
- PENDING/COMPLETED tasks are untouched
- `flyway_schema_history` records V3 as successfully applied
