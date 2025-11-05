package uk.gov.hmcts.marketplace.postgreslock.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.marketplace.postgreslock.config.TestContainersInitialise;
import uk.gov.hmcts.marketplace.postgreslock.repository.AuditRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(TestContainersInitialise.class)
@ContextConfiguration(initializers = TestContainersInitialise.class)
@AutoConfigureMockMvc
@Slf4j
class AuditIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @Autowired
    AuditRepository auditRepository;

    @Transactional
    @Test
    void todo() throws Exception {
    }
}