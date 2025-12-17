package uk.gov.hmcts.cp.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@AllArgsConstructor
@Slf4j
public class DemoService {

    ClockService clockService;

    public String methodThatNeedsDate() {
        OffsetDateTime now = clockService.now();
        log.info("now is {}", now);
        return now.format(DateTimeFormatter.ISO_DATE);
    }

    public String methodThatNeedsTime() {
        OffsetDateTime now = clockService.now();
        log.info("now is {}", now);
        return now.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
