package uk.gov.hmcts.audit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uk.gov.hmcts.audit.service.AuditService;

@Configuration
public class AuditFilterConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public AuditFilter auditFilter(
            @Qualifier("requestMappingHandlerMapping") final RequestMappingHandlerMapping handlerMapping,
            final AuditService auditService) {
        return new AuditFilter(handlerMapping, auditService);
    }
}
