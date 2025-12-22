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

    @Test
    void real_clock_should_used() {
        log.info("This test will mock http response from endpoint and" +
                "hit our controller through mockMvc then through service and into our http client" +
                "Which will hit the mock endpoint" +
                "The only new thing to decide is whether to use RestClient WebCLient RestTemplate" +
                "And work out whether we need to do any certs work")
    }
}
