package uk.gov.hmcts.marketplace.postgres.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerEntity;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnswerMapper {

    // we need to explicitly map this because it has a different field name
    @Mapping(source = "answerText", target = "answer")
    public abstract AnswerResponse mapAnswer(AnswerEntity answerEntity);
}
