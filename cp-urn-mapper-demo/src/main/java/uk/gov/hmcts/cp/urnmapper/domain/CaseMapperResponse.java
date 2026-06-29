package uk.gov.hmcts.cp.urnmapper.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CaseMapperResponse {
    private String caseId;
    private String caseUrn;
}
