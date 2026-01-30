import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class DemoDockerTest {

    @Test
    void int_test_should_start_docker_and_springboot_and_run_ok(){
        log.info("Docker test running .. without docker");
        throw new RuntimeException("Bad integration test");
    }
}
