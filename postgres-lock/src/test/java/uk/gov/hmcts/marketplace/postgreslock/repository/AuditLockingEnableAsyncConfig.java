package uk.gov.hmcts.marketplace.postgreslock.repository;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AuditLockingEnableAsyncConfig {
    // We need this to enable async processing ... so we can easily test hammering the postgres audits
}
