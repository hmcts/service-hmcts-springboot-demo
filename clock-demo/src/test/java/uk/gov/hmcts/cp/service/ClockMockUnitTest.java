package uk.gov.hmcts.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ClockMockUnitTest {

    @Mock
    ClockService clockService;

    @InjectMocks
    DemoService demoService;

    private final static OffsetDateTime MOCKNOW = OffsetDateTime.of(2025, 12, 1, 11, 30, 59, 0, ZoneOffset.UTC);

    @Test
    void mock_clock_should_return_date_and_time() {
        when(clockService.now()).thenReturn(MOCKNOW);
        assertThat(demoService.methodThatNeedsDate()).isEqualTo("2025-12-01Z");

        assertThat(demoService.methodThatNeedsTime()).isEqualTo("11:30");
    }
}
