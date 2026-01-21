import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class DemoIntegrationTest {

    @Test
    void int_test_should_start_docker_and_run_ok(){
        log.info("Integration test running");
        throw new RuntimeException("BAd");
    }
}
