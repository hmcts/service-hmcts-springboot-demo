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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Slf4j
class AnswerRepositoryTest {

    @Autowired
    AnswerRepository answerNewRepository;

    OffsetDateTime today = OffsetDateTime.now();
    OffsetDateTime tomorrow = today.plusDays(1);

    @BeforeEach
    void beforeEach() {
        clearDownData();
    }

    @Test
    void query_should_save_and_return_entity_by_id() {
        AnswerEntity saved = answerNewRepository.save(newAnswer("the answer"));
        AnswerEntity read = answerNewRepository.findById(saved.getAnswerId()).get();
        assertThat(read.getAnswerId()).isNotNull();
        assertThat(read.getCreatedAt()).isNotNull();
        log.info("Saved and retrieved answer with id:{} and createdAt:{}", read.getAnswerId(), read.getCreatedAt());
    }

    @Test
    void query_should_query_by_caseId() {
        AnswerEntity saved = answerNewRepository.save(newAnswer("the answer"));
        List<AnswerEntity> answers = answerNewRepository.findByCaseId(saved.getCaseId());
        assertThat(answers).hasSize(1);
    }

    @Test
    void query_should_find_latest_for_caseId() {
        AnswerEntity saved1 = answerNewRepository.save(newAnswer("answer1"));
        AnswerEntity saved2 = answerNewRepository.save(newAnswer("answer2"));
        Optional<AnswerEntity> answer1 = answerNewRepository.findFirstByQueryIdOrderByCreatedAtDesc(saved1.getQueryId());
        assertThat(answer1.get().getAnswer()).isEqualTo("answer1");

        Optional<AnswerEntity> answer2 = answerNewRepository.findFirstByQueryIdOrderByCreatedAtDesc(saved2.getQueryId());
        assertThat(answer2.get().getAnswer()).isEqualTo("answer2");
    }

    @Test
    void query_should_get_count_for_queryId() {
        AnswerEntity saved = answerNewRepository.save(newAnswer("answer1"));
        long count = answerNewRepository.countDistinctByQueryId(saved.getQueryId());
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void query_should_not_get_future_answer() {
        AnswerEntity saved = answerNewRepository.save(newAnswer("answer1"));
        List<AnswerEntity> result = answerNewRepository.findLatestBeforeGivenTime(saved.getCaseId(), saved.getQueryId(), today);
        assertThat(result).hasSize(0);
    }

    @Test
    void query_should_get_old_answer() {
        AnswerEntity saved = answerNewRepository.save(newAnswer("answer1"));
        List<AnswerEntity> result = answerNewRepository.findLatestBeforeGivenTime(saved.getCaseId(), saved.getQueryId(), tomorrow);
        assertThat(result).hasSize(1);
    }

    private void clearDownData() {
        answerNewRepository.deleteAll();
    }

    private AnswerEntity newAnswer(String answer) {
        return AnswerEntity.builder()
                .caseId(UUID.randomUUID())
                .queryId(UUID.randomUUID())
                .version(1L)
                .answer(answer)
                .build();
    }
}