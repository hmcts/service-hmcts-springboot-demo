package uk.gov.hmcts.marketplace.postgres.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerResponse;
import uk.gov.hmcts.marketplace.postgres.mapper.AnswerMapper;
import uk.gov.hmcts.marketplace.postgres.repository.AnswerRepository;

import java.util.Optional;

@Service
@AllArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final AnswerMapper answerMapper;

    public AnswerResponse getAnswer(long answerId) {
        Optional<AnswerEntity> an1 = answerRepository.findById(answerId);
        AnswerEntity answerEntity = answerRepository.getReferenceById(answerId);
        return answerMapper.mapAnswer(answerEntity);
    }
}
