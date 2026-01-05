package uk.gov.hmcts.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class InstantToLocalDateTimeConversionsTest {

    @Test
    void ldt_should_convert_to_instant() {
        LocalDateTime ldt = LocalDateTime.of(2025, 1, 20, 12, 30);
        Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
        assertThat(instant.toString()).endsWith("12:30:00Z");
    }

    @Test
    void instant_should_convert_to_ldt() {
        Instant instant = Instant.parse("2025-08-01T09:30:00Z");
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault());
        assertThat(ldt.toString()).isEqualTo("2025-08-01T10:30");
    }
}
