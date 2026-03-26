package uk.gov.hmcts.cp.demo.audit.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class ArtemisAuditConfig {

    public static final String AUDIT_QUEUE_NAME = "audit-log";

    private static final Logger log = LoggerFactory.getLogger(ArtemisAuditConfig.class);

    private final JmsTemplate jmsTemplate;

    public ArtemisAuditConfig(@Autowired(required = false) JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (jmsTemplate != null) {
            ArtemisAuditAppenderHolder.setSend(jmsTemplate, AUDIT_QUEUE_NAME);
            log.info("Audit logs will be sent to Artemis queue: {}", AUDIT_QUEUE_NAME);
        } else {
            log.warn("Artemis audit logging disabled: no JMS connection (JmsTemplate not available)");
        }
    }
}
