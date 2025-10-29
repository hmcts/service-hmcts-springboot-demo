package uk.gov.hmcts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
@Slf4j
public class DemoApplication {
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
