package uk.gov.hmcts.marketplace.postgres.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class AnswerResponse {

    private UUID queryId;
    private String userQuery;
    private String answer;
    private Integer version;
}
