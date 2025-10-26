package uk.gov.hmcts.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("uk.gov.hmcts")
public class AuditConfiguration {
    // This class scans for AuditFilter under the uk.gov.hmcts package
}