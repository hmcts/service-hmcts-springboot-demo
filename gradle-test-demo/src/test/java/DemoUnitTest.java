import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class DemoUnitTest {

    @Test
    void unit_test_should_run_for_test_task() {
        log.info("Unit Test Running");
        assertThat("a").isEqualTo("a");
    }
}
