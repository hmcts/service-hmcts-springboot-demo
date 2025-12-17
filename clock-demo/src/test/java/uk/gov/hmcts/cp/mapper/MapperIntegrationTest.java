package uk.gov.hmcts.cp.mapper;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.cp.model.DemoRequest;
import uk.gov.hmcts.cp.service.ClockService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.Mockito.when;

@SpringBootTest
@Slf4j
class MapperIntegrationTest {

    @MockitoBean
    ClockService clockService;

    @Autowired
    DemoMapper demoMapper;

    private final static OffsetDateTime MOCKNOW = OffsetDateTime.of(2025, 12, 1, 11, 30, 59, 0, ZoneOffset.UTC);

    @Test
    void mock_clock_should_return_date_and_time() {
        when(clockService.now()).thenReturn(MOCKNOW);
        demoMapper.mapToResponse(clockService, DemoRequest.builder().build());
    }
}
