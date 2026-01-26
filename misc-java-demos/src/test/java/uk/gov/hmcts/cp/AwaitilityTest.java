package uk.gov.hmcts.cp;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class AwaitilityTest {

    int counter = 0;

    @Test
    void async_process_should_proceed_if_finished() {
        await()
                .atMost(Duration.ofSeconds(5))
                .until(this::process_has_completed);
        log.info("Process has finished");
    }

    @Test
    void async_process_should_timeout_with_exception_if_too_slow() {
        ///  default pollING interval is 100 msecs
        ConditionTimeoutException e = assertThrows(ConditionTimeoutException.class,
                () -> await()
                        .atMost(Duration.ofMillis(101))
                        .until(this::process_has_completed));
        assertThat(e.getMessage()).contains("was not fulfilled within 101 milliseconds.");
    }

    @Test
    void sleepy_wait_between_checks_should_pass() {
        await()
                .atMost(Duration.ofSeconds(4))
                .with()
                .pollInterval(Duration.ofSeconds(1))
                .until(this::process_has_completed);
        log.info("Process has finished");
    }

    private boolean process_has_completed() {
        log.info("... checking if {} process has started / completed / whatever we want to wait for", counter);
        return counter++ >= 2;
    }
}
