package uk.gov.hmcts.cp.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class AmpResponse {
    private Long exampleId;
    private String exampleText;
}
