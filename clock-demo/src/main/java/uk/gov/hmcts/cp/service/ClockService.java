package uk.gov.hmcts.cp.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
public class ClockService {

    private Clock clock;

    // we use a ClockService to expose the clock time in a simple method
    public OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
