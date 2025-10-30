package uk.gov.hmcts.marketplace.postgres.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class AnswerResponse {

    private long id;
    private UUID caseId;
    private String answer;
}
