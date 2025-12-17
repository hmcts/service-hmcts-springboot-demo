package uk.gov.hmcts.cp.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.cp.model.DemoRequest;
import uk.gov.hmcts.cp.model.DemoResponse;
import uk.gov.hmcts.cp.service.ClockService;

@Mapper(componentModel = "spring")
public abstract class DemoMapper {


    @Mapping(target = "createdAt", expression = "java(clockService.now())")
    public abstract DemoResponse mapToResponse(@Context ClockService clockService, DemoRequest request);
}
