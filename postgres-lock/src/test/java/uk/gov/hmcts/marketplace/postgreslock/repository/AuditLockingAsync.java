package uk.gov.hmcts.marketplace.postgreslock.repository;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.marketplace.postgreslock.entity.AuditEntity;

import java.util.Optional;
import java.util.Random;

@Component
@Slf4j
@Getter
public class AuditLockingAsync {
    @Autowired
    private AuditRepository auditRepository;

    private int auditsSent;

    @SneakyThrows
    @Async
    @Transactional
    public void processAuditAsync() {
        log.info("Audit has {} entries", auditRepository.count());
        Optional<AuditEntity> next = auditRepository.findNextAudit();
        if (next.isPresent()) {
            int sleepMsecs = new Random().nextInt(10);
            log.info("Sleeping for {} msecs to emulate sending record:{} to artemis", sleepMsecs, next.get().getId());
            Thread.sleep(sleepMsecs);
            auditRepository.delete(next.get());
            auditsSent++;
        } else {
            log.info("No audit found");
        }
    }
}
