package uk.gov.hmcts.marketplace.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.marketplace.config.RetryConfigService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {
    @Mock
    RetryConfigService retryConfigService;
    @Mock
    ClockService clockService;

    @InjectMocks
    RetryService retryService;

    @Test
    void retry_delay_should_be_correct() {
        when(retryConfigService.getRetryDelaySeconds()).thenReturn(List.of(1, 2));

        assertThat(retryService.getDelaySecs(0)).isEqualTo(1);
        assertThat(retryService.getDelaySecs(1)).isEqualTo(2);
        assertThat(retryService.getDelaySecs(2)).isEqualTo(2);
    }

    @Test
    void retry_time_should_be_correct() {
        OffsetDateTime now = OffsetDateTime.now();
        when(retryConfigService.getRetryDelaySeconds()).thenReturn(List.of(1, 3600));
        when(clockService.now()).thenReturn(now);

        assertThat(retryService.getNextTryTime(0)).isEqualTo(now.plusSeconds(1));
        assertThat(retryService.getNextTryTime(1)).isEqualTo(now.plusSeconds(3600));
        assertThat(retryService.getNextTryTime(2)).isEqualTo(now.plusSeconds(3600));
    }
}