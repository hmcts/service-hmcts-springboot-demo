package uk.gov.hmcts.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class InstantToZoneDateTimeConversionsTest {

    @Test
    void zdt_should_convert_to_instant() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 1, 20, 12, 30, 1, 123456789, ZoneOffset.UTC);
        Instant instant = zdt.toInstant();
        assertThat(instant.toString()).isEqualTo("2025-01-20T12:30:01.123456789Z");
    }

    @Test
    void instant_should_convert_to_zdt() {
        Instant instant = Instant.parse("2025-08-01T09:30:00Z");
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.systemDefault());
        assertThat(zdt.toString()).isEqualTo("2025-08-01T10:30+01:00[Europe/London]");
    }
}
