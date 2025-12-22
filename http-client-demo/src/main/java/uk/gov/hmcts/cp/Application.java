package uk.gov.hmcts.cp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = "uk.gov.hmcts.cp")
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}