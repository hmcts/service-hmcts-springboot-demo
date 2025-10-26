package uk.gov.hmcts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@SpringBootApplication
@Slf4j
public class DemoApplication {
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
