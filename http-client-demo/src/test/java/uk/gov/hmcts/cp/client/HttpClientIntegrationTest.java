package uk.gov.hmcts.cp.client;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
class HttpClientIntegrationTest {

    @Test
    void client_should_get_expected_response() {
        log.info("This test will mock http response from target endpoint and" +
                "hit our client directly and then hit the mock endpoint" +
                "The only new thing to decide is whether to use RestClient WebClient RestTemplate Feign" +
                "And work out whether we need to do any certs work");
    }
}
