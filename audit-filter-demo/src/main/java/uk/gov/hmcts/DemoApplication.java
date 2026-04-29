package uk.gov.hmcts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(
    basePackages = "uk.gov.hmcts",
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "uk\\.gov\\.hmcts\\.cp\\..*")
)
@Slf4j
public class DemoApplication {
    public static void main(final String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
