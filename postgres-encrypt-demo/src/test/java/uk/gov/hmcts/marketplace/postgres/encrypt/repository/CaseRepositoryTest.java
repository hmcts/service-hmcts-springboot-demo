package uk.gov.hmcts.marketplace.postgres.encrypt.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.marketplace.postgres.encrypt.config.TestContainersInitialise;
import uk.gov.hmcts.marketplace.postgres.encrypt.domain.CaseEntity;
import uk.gov.hmcts.marketplace.postgres.encrypt.domain.Defendent;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(TestContainersInitialise.class)
@ContextConfiguration(initializers = TestContainersInitialise.class)
class CaseRepositoryTest {

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    LocalDate dateOfBirth = LocalDate.of(2000,1,1);
    Defendent defendent = Defendent.builder()
            .name("Jane Doe")
            .dateOfBirth(dateOfBirth)
            .build();
    CaseEntity caseEntity = CaseEntity.builder()
            .caseReference("REF-001")
            .secureText("John Smith")
            .defendent(defendent)
            .build();

    @Test
    void secure_fields_should_be_encrypted_in_database_and_decrypted_on_load() {
        CaseEntity saved = caseRepository.save(caseEntity);

        assertEncrypted(saved.getId(), "secure_text");
        assertEncrypted(saved.getId(), "defendent_json");

        CaseEntity read = caseRepository.findById(saved.getId()).get();
        assertThat(read.getSecureText()).isEqualTo("John Smith");
        assertThat(read.getDefendent().getName()).isEqualTo("Jane Doe");
        assertThat(read.getDefendent().getDateOfBirth()).isEqualTo(dateOfBirth);
    }

    private void assertEncrypted(long id, String fieldName) {
        assertThat(getFieldFromDatabase(id, fieldName)).startsWith("<ENCRYPTED>").endsWith("</ENCRYPTED>");
    }

    private String getFieldFromDatabase(long id, String fieldName) {
        String query = String.format("select %s from hmcts_case where id=%d", fieldName, id);
        return jdbcTemplate.queryForObject(query, String.class);
    }
}
