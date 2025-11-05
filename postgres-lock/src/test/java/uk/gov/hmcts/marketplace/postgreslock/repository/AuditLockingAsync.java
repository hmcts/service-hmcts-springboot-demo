package uk.gov.hmcts.marketplace.postgreslock.repository;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.marketplace.postgreslock.service.AuditService;

@Component
@Slf4j
@Getter
public class AuditLockingAsync {
    @Autowired
    private AuditService auditService;

    @SneakyThrows
    @Async
    public void processAuditAsync() {
        auditService.sendAudits();
    }
}
