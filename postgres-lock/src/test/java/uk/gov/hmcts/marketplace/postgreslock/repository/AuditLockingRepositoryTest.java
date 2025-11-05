package uk.gov.hmcts.marketplace.postgreslock.repository;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.marketplace.postgreslock.config.TestContainersInitialise;
import uk.gov.hmcts.marketplace.postgreslock.entity.AuditEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(TestContainersInitialise.class)
@ContextConfiguration(initializers = TestContainersInitialise.class)
@Slf4j
class AuditLockingRepositoryTest {

    @Autowired
    AuditRepository auditRepository;
    @Autowired
    AuditLockingAsync auditLockingAsync;

    @BeforeEach
    void beforeEach() {
        auditRepository.deleteAll();
    }

    int numberOfAudits = 100;
    int numberOfExtraChecks = 5;

    @SneakyThrows
    @AfterEach
    void after() {
        log.info("Count After:{}", auditRepository.count());
        // This is a bit rubbish we need to wait for all threads to finish of course
        Thread.sleep(10000);
        log.info("Count After:{}", auditRepository.count());
        assertThat(auditLockingAsync.getAuditsSent()).isEqualTo(numberOfAudits);
        assertThat(auditLockingAsync.getAuditsNotFound()).isEqualTo(numberOfExtraChecks);
    }


    @Test
    void getNextAudit_should_lock_rows_exactly_100_sends() {
        insertAudits(numberOfAudits);
        log.info("Count Before:{}", auditRepository.count());
        for (int n = 0; n < numberOfAudits + numberOfExtraChecks; n++) {
            auditLockingAsync.processAuditAsync();
        }
    }

    private void insertAudits(int numberOfAudits) {
        for (int n = 0; n < numberOfAudits; n++) {
            String payload = String.format("Payload %05d", n);
            auditRepository.saveAndFlush(AuditEntity.builder().payload(payload).build());
        }
    }
}