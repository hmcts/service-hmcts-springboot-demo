package uk.gov.hmcts.marketplace.postgres.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Slf4j
class AnswerRepositoryTest {

    @Autowired
    AnswerRepository answerRepository;

    @BeforeEach
    void beforeEach() {
        clearDownData();
    }

    @Test
    void query_should_save_and_return_entity_by_id() {
        AnswerEntity saved = answerRepository.save(newAnswer("the answer"));
        AnswerEntity read = answerRepository.findById(saved.getId()).get();
        assertThat(read.getId()).isNotNull();
        assertThat(read.getAnswerText()).isEqualTo("the answer");
    }

    @Test
    void query_should_query_by_caseId() {
        AnswerEntity saved = answerRepository.save(newAnswer("the answer"));
        List<AnswerEntity> answers = answerRepository.queryExample(saved.getCaseId());
        assertThat(answers).hasSize(1);
    }

    @Test
    void query_should_get_count_for_queryId() {
        AnswerEntity saved = answerRepository.save(newAnswer("answer1"));
        AnswerEntity answer2 = newAnswer("answer2").toBuilder().caseId(saved.getCaseId()).build();
        answerRepository.save(answer2);
        long count = answerRepository.countDistinctByCaseId(saved.getCaseId());
        assertThat(count).isEqualTo(2L);
    }

    private void clearDownData() {
        answerRepository.deleteAll();
    }

    private AnswerEntity newAnswer(String answer) {
        return AnswerEntity.builder()
                .caseId(UUID.randomUUID())
                .answerText(answer)
                .build();
    }
}