package uk.gov.hmcts.cp.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class InstantBstTest {

    // We use Europe/London timezone to ensure it follows BST
    ZoneId zoneId = ZoneId.of("Europe/London");
    LocalTime time = LocalTime.of(10, 30);

    @Test
    void ten_thirty_in_winter_should_be_1030_utc_instant() {
        ZonedDateTime zdtWinter = ZonedDateTime.of(LocalDate.of(2020, 2, 20), time, zoneId);
        Instant instantWinter = zdtWinter.toInstant();
        assertThat(instantWinter.toString()).endsWith("10:30:00Z");
    }

    @Test
    void ten_thirty_in_summer_should_be_0930_utc_instant() {
        ZonedDateTime zdtSummer = ZonedDateTime.of(LocalDate.of(2020, 7, 20), time, zoneId);
        Instant instantSummer = zdtSummer.toInstant();
        assertThat(instantSummer.toString()).endsWith("09:30:00Z");
    }

    @Test
    void nine_thirty_summer_utc_instant_should_be_ten_thirty_bst() {
        Instant instantSummer = Instant.parse("2025-08-01T09:30:00Z");
        LocalDateTime ldt = LocalDateTime.ofInstant(instantSummer, ZoneOffset.systemDefault());
        assertThat(ldt.toString()).endsWith("10:30");
    }
}
