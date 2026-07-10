package uk.gov.hmcts.audit.misc.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@AllArgsConstructor
public class ClockService {

    private final Clock clock;

    public Instant now() {
        return clock.instant();
    }
}
