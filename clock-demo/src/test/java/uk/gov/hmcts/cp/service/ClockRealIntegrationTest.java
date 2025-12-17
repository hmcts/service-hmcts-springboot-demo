package uk.gov.hmcts.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
class ClockRealIntegrationTest {

    @Autowired
    DemoService demoService;

    @Test
    void real_clock_should_used() {
        String today = OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE);
        assertThat(demoService.methodThatNeedsDate()).isEqualTo(today);

        String time = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        assertThat(demoService.methodThatNeedsTime()).isEqualTo(time);
    }
}
