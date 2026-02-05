package uk.gov.hmcts.cp.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.cp.model.DemoRequest;
import uk.gov.hmcts.cp.model.DemoResponse;
import uk.gov.hmcts.cp.service.ClockService;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public abstract class DemoMapper {


    @Mapping(target = "createdAt", expression = "java(clockService.now())")
    public abstract DemoResponse mapToResponseWithClock(@Context ClockService clockService, DemoRequest request);

    @Mapping(target = "createdAt", source = "createdAt")
    public abstract DemoResponse mapToResponseWithInstant(OffsetDateTime createdAt, DemoRequest request);
}
