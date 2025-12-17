package uk.gov.hmcts.cp.model;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Builder
@Getter
public class DemoResponse {

    OffsetDateTime createdAt;
}
