package uk.gov.hmcts.cp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.openapi.model.UrnMapperResponse;

@Mapper(componentModel = "spring")
public interface UrnMapperResponseMapper {

    @Mapping(source = "targetId", target = "caseId")
    @Mapping(source = "sourceId", target = "caseUrn")
    CaseMapperResponse toCaseMapperResponse(UrnMapperResponse urnMapperResponse);
}
