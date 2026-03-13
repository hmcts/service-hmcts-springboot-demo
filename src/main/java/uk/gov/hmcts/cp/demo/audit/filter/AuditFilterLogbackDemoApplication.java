package uk.gov.hmcts.cp.demo.audit.filter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class AuditFilterLogbackDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditFilterLogbackDemoApplication.class, args);
    }

}
