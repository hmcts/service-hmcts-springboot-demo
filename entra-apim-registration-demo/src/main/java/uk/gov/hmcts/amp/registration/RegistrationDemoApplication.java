package uk.gov.hmcts.amp.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RegistrationDemoApplication {

    public static void main(final String[] args) {
        SpringApplication.run(RegistrationDemoApplication.class, args);
    }
}
