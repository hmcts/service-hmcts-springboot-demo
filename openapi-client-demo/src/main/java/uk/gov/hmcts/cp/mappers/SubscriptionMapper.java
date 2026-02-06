package uk.gov.hmcts.cp.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.cp.openapi.model.ExampleResponse;
import uk.gov.hmcts.cp.service.AmpResponse;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    @Mapping(source = "exampleId", target = "exampleId")
    AmpResponse mapClientResponse(ExampleResponse clientResponse);
}
