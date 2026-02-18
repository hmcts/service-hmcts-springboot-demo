package uk.gov.hmcts.marketplace.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.config.RetryConfigService;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class RetryService {

    private RetryConfigService retryConfigService;
    private ClockService clockService;

    public int getDelaySecs(int failureCount) {
        List<Integer> retryConfig = retryConfigService.getRetryDelaySeconds();
        int retryIndex = failureCount < retryConfig.size() ? failureCount : retryConfig.size() - 1;
        int retryDelaySecs = retryConfig.get(retryIndex);
        log.info("retry delay {} seconds", retryDelaySecs);
        return retryDelaySecs;
    }

    public OffsetDateTime getNextTryTime(int failureCount) {
        return clockService.now().plusSeconds(getDelaySecs(failureCount));
    }
}
