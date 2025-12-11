package uk.gov.hmcts.marketplace.postgres.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.marketplace.postgres.config.TestContainersInitialise;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;
import uk.gov.hmcts.marketplace.postgres.repository.AnswerRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(TestContainersInitialise.class)
@ContextConfiguration(initializers = TestContainersInitialise.class)
@AutoConfigureMockMvc
@Slf4j
class AnswerIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @Autowired
    AnswerRepository answerRepository;

    @Transactional
    @Test
    void answer_endpoint_should_return_answer() throws Exception {
        AnswerEntity existingAnswer = insertAnswer();
        log.info("Saved answer with id:{}", existingAnswer.getId());
        mockMvc
                .perform(get("/answer/{id}", existingAnswer.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(existingAnswer.getId()))
                .andExpect(jsonPath("caseId").value(existingAnswer.getCaseId().toString()))
                .andExpect(jsonPath("answer").value("The answer"));
    }

    private AnswerEntity insertAnswer() {
        AnswerEntity answerEntity = AnswerEntity.builder()
                .caseId(UUID.randomUUID())
                .answerText("The answer")
                .build();
        return answerRepository.save(answerEntity);
    }
}