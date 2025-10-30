package uk.gov.hmcts.marketplace.postgres.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerMapperTest {

    AnswerMapper answerMapper = new AnswerMapperImpl();

    AnswerEntity answerEntity = AnswerEntity.builder()
            .id(21L)
            .caseId(UUID.randomUUID())
            .answerText("some text")
            .build();

    @Test
    void answer_entity_should_map_to_response() {
        AnswerResponse response = answerMapper.mapAnswer(answerEntity);
        assertThat(response.getId()).isEqualTo(answerEntity.getId());
        assertThat(response.getCaseId()).isEqualTo(answerEntity.getCaseId());
        assertThat(response.getAnswer()).isEqualTo("some text");
    }
}