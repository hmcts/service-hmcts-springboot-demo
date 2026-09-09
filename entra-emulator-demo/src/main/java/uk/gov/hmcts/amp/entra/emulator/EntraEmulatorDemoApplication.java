package uk.gov.hmcts.amp.entra.emulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EntraEmulatorDemoApplication {

    public static void main(final String[] args) {
        SpringApplication.run(EntraEmulatorDemoApplication.class, args);
    }
}
