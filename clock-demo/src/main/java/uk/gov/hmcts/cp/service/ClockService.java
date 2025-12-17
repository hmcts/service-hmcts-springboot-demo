package uk.gov.hmcts.cp.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@AllArgsConstructor
public class ClockService {

    Clock clock;

    // we use a ClockService to expose the clock time in a simple method
    public OffsetDateTime now() {
        return clock.instant().atOffset(ZoneOffset.UTC);
    }
}
