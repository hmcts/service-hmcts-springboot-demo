package uk.gov.hmcts.cp.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.openapi.model.UrnMapperResponse;

import static org.assertj.core.api.Assertions.assertThat;

class UrnMapperResponseMapperTest {

    private final UrnMapperResponseMapper mapper = new UrnMapperResponseMapperImpl();

    @Test
    void mapping_urn_mapper_response_should_set_case_id_from_target_id() {
        UrnMapperResponse urnMapperResponse = new UrnMapperResponse()
                .targetId("37326f3d-15b2-44b3-820e-a8df199f6f80")
                .sourceId("28DI5874594")
                .mappingId("e345e897-301e-44c7-85ee-f0c9e8d1e4af")
                .sourceType("OU_URN")
                .targetType("CASE_FILE_ID");

        CaseMapperResponse result = mapper.toCaseMapperResponse(urnMapperResponse);

        assertThat(result.getCaseId()).isEqualTo("37326f3d-15b2-44b3-820e-a8df199f6f80");
        assertThat(result.getCaseUrn()).isEqualTo("28DI5874594");
    }
}
