package uk.gov.hmcts.cp.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.mapper.DemoMapper;
import uk.gov.hmcts.cp.model.DemoRequest;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@AllArgsConstructor
@Slf4j
public class DemoService {

    ClockService clockService;
    DemoMapper demoMapper;

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

    public String callMapperMethod() {
        DemoRequest request = DemoRequest.builder().build();
        demoMapper.mapToResponseWithClock(clockService, request);
        return "Something";
    }
}
