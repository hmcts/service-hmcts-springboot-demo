package uk.gov.hmcts.marketplace.postgres.encrypt.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.marketplace.postgres.encrypt.config.TestContainersInitialise;
import uk.gov.hmcts.marketplace.postgres.encrypt.domain.CaseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that @Encrypted fields are stored encrypted in the database
 * and decrypted transparently when loaded through the repository.
 */
@SpringBootTest
@ExtendWith(TestContainersInitialise.class)
@ContextConfiguration(initializers = TestContainersInitialise.class)
class CaseRepositoryTest {

    private static final String DEFENDANT = "John Smith";

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldEncryptDefendantNameInDatabase() {
        CaseEntity saved = caseRepository.save(
            CaseEntity.builder()
                .caseReference("REF-001")
                .defendantName(DEFENDANT)
                .build()
        );

        String rawDbValue = jdbcTemplate.queryForObject(
            "select defendant_name from hmcts_case where id = ?",
            String.class,
            saved.getId()
        );

        assertThat(rawDbValue)
            .as("defendant name must not be stored as plain text")
            .isNotEqualTo(DEFENDANT);
    }

    @Test
    void shouldDecryptDefendantNameOnLoad() {
        CaseEntity saved = caseRepository.save(
            CaseEntity.builder()
                .caseReference("REF-002")
                .defendantName(DEFENDANT)
                .build()
        );

        caseRepository.flush();

        CaseEntity loaded = caseRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getDefendantName())
            .as("defendant name must be decrypted transparently on load")
            .isEqualTo(DEFENDANT);
    }
}
