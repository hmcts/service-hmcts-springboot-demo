package uk.gov.hmcts.marketplace.postgres.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerResponse;
import uk.gov.hmcts.marketplace.postgres.mapper.AnswerMapper;
import uk.gov.hmcts.marketplace.postgres.repository.AnswerRepository;

@Service
@AllArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final AnswerMapper answerMapper;

    public AnswerResponse getAnswer(long answerId) {
        AnswerEntity answerEntity = answerRepository.getById(answerId);
        return answerMapper.mapAnswer(answerEntity);
    }
}
