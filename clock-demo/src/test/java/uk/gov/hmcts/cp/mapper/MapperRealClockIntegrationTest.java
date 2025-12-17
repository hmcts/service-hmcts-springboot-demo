package uk.gov.hmcts.cp.mapper;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.model.DemoRequest;
import uk.gov.hmcts.cp.model.DemoResponse;
import uk.gov.hmcts.cp.service.ClockService;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
class MapperRealClockIntegrationTest {

    @Autowired
    ClockService clockService;
    @Autowired
    DemoMapper demoMapper;

    @Test
    void mock_clock_should_return_date_and_time() {
        OffsetDateTime startTime = OffsetDateTime.now();
        log.info("starteddAt:{}", startTime);
        DemoResponse response = demoMapper.mapToResponse(clockService, DemoRequest.builder().build());
        log.info("createdAt:{}", response.getCreatedAt());
        assertThat(response.getCreatedAt()).isAfter(startTime);
    }
}
