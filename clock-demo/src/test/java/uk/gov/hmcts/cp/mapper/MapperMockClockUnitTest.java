package uk.gov.hmcts.cp.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.model.DemoRequest;
import uk.gov.hmcts.cp.model.DemoResponse;
import uk.gov.hmcts.cp.service.ClockService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapperMockClockUnitTest {

    @Mock
    ClockService clockService;

    DemoMapper demoMapper = new DemoMapperImpl();

    private final static OffsetDateTime MOCKNOW = OffsetDateTime.of(2025, 12, 1, 11, 30, 59, 0, ZoneOffset.UTC);

    @Test
    void mapper_should_use_mock_datetime_from_clock() {
        when(clockService.now()).thenReturn(MOCKNOW);
        DemoResponse demoResponse = demoMapper.mapToResponseWithClock(clockService, DemoRequest.builder().build());
        assertThat(demoResponse.getCreatedAt()).isEqualTo(MOCKNOW);
    }

    @Test
    void mapper_should_use_passed_in_datetime() {
        DemoResponse demoResponse = demoMapper.mapToResponseWithInstant(MOCKNOW, DemoRequest.builder().build());
        assertThat(demoResponse.getCreatedAt()).isEqualTo(MOCKNOW);
    }
}