package uk.gov.hmcts.cp.demo.audit.filter;

import org.springframework.jms.core.JmsTemplate;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holder for the JMS send function used by {@link ArtemisAuditAppender}.
 * Set by {@link ArtemisAuditConfig} when the Spring context is ready.
 */
public final class ArtemisAuditAppenderHolder {

    private static final AtomicReference<SendFunction> SEND_REF = new AtomicReference<>();

    @FunctionalInterface
    public interface SendFunction {
        void send(String message);
    }

    private ArtemisAuditAppenderHolder() {
    }

    public static void setSend(JmsTemplate jmsTemplate, String queueName) {
        SEND_REF.set(message -> jmsTemplate.convertAndSend(queueName, message));
    }

    static SendFunction getSend() {
        return SEND_REF.get();
    }
}
