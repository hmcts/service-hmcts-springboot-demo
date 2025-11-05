package uk.gov.hmcts.marketplace.postgreslock.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.marketplace.postgreslock.config.TestContainersInitialise;
import uk.gov.hmcts.marketplace.postgreslock.entity.AuditEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(TestContainersInitialise.class)
@ContextConfiguration(initializers = TestContainersInitialise.class)
@Slf4j
@Transactional
class AuditRepositoryTest {

    @Autowired
    AuditRepository auditRepository;

    @BeforeEach
    void beforeEach() {
        auditRepository.deleteAll();
    }

    @Test
    void saved_audit_should_be_readable() {
        AuditEntity saved = insertAudit("{}");
        assertThat(auditRepository.getReferenceById(saved.getId()).getPayload()).isEqualTo("{}");
    }

    @Test
    void getNextAudit_should_return_empty_if_non() {
        assertThat(auditRepository.findNextAudit()).isEmpty();
    }

    @Test
    void getNextAudit_should_return_correct_entity() {
        insertAudit("{}");
        insertAudit("something else");
        assertThat(auditRepository.findNextAudit().get().getPayload()).isEqualTo("{}");
    }

    private AuditEntity insertAudit(String payload) {
        return auditRepository.save(AuditEntity.builder().payload(payload).build());
    }
}