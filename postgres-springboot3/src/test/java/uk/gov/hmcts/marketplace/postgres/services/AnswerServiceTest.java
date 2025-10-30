package uk.gov.hmcts.marketplace.postgres.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerResponse;
import uk.gov.hmcts.marketplace.postgres.mapper.AnswerMapper;
import uk.gov.hmcts.marketplace.postgres.repository.AnswerRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {
    @Mock
    AnswerRepository answerRepository;
    @Mock
    AnswerMapper answerMapper;

    @InjectMocks
    AnswerService answerService;

    AnswerEntity answerEntity = AnswerEntity.builder()
            .answerText("some text")
            .build();
    AnswerResponse answerResponse = AnswerResponse.builder()
            .answer("some text")
            .build();

    @Test
    void get_answer_should_retrieve_and_map_response() {
        when(answerRepository.getReferenceById(2L)).thenReturn(answerEntity);
        when(answerMapper.mapAnswer(answerEntity)).thenReturn(answerResponse);
        AnswerResponse actualResponse = answerService.getAnswer(2L);
        assertThat(actualResponse).isEqualTo(answerResponse);
    }
}