package uk.gov.hmcts.marketplace.postgreslock.integration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.marketplace.postgreslock.client.ArtemisClient;
import uk.gov.hmcts.marketplace.postgreslock.config.TestContainersInitialise;
import uk.gov.hmcts.marketplace.postgreslock.entity.AuditEntity;
import uk.gov.hmcts.marketplace.postgreslock.repository.AuditLockingAsync;
import uk.gov.hmcts.marketplace.postgreslock.repository.AuditRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = TestContainersInitialise.class)
@Slf4j
class AuditIntegrationTest {

    @Autowired
    AuditRepository auditRepository;
    @Autowired
    AuditLockingAsync auditLockingAsync;
    @MockitoBean
    ArtemisClient artemisClient;

    @BeforeEach
    void beforeEach() {
        auditRepository.deleteAll();
    }

    int numberOfAudits = 100;


    @SneakyThrows
    @Test
    void failure_to_send_audit_should_retry() {
        insertAudits(numberOfAudits);
        log.info("Count Before:{}", auditRepository.count());
        for (int n = 0; n < numberOfAudits + 10; n++) {
            auditLockingAsync.processAuditAsync();
        }
        // This is a bit rubbish we should wait for all threads to finish of course
        Thread.sleep(10000);
        verify(artemisClient, times(numberOfAudits)).sentToArtemis(anyString());
        assertThat(auditRepository.count()).isEqualTo(0);
    }

    private void insertAudits(int numberOfAudits) {
        for (int n = 0; n < numberOfAudits; n++) {
            String payload = String.format("Payload %05d", n);
            auditRepository.saveAndFlush(AuditEntity.builder().payload(payload).build());
        }
    }
}