package uk.gov.hmcts.integration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.gov.hmcts.cp.audit.AuditFilter;
import uk.gov.hmcts.cp.audit.service.AuditDecisionService;
import uk.gov.hmcts.cp.audit.service.AuditPayloadGenerationService;
import uk.gov.hmcts.cp.audit.service.AuditSenderService;
import uk.gov.hmcts.cp.audit.service.AuditService;

@TestConfiguration
class TestAuditConfig {

    @Bean
    AuditDecisionService auditDecisionService() {
        return new AuditDecisionService();
    }

    @Bean
    AuditPayloadGenerationService auditPayloadGenerationService() {
        return new AuditPayloadGenerationService();
    }

    @Bean
    AuditService auditService(final AuditPayloadGenerationService payloadService,
                              final AuditSenderService senderService) {
        return new AuditService(payloadService, senderService);
    }

    @Bean
    FilterRegistrationBean<AuditFilter> auditFilterRegistration(
            @Qualifier("requestMappingHandlerMapping") final RequestMappingHandlerMapping handlerMapping,
            final AuditDecisionService decisionService,
            final AuditService auditService) {
        final FilterRegistrationBean<AuditFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuditFilter(handlerMapping, decisionService, auditService));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
