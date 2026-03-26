package uk.gov.hmcts.cp.demo.audit.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Logback appender that sends audit log messages to an Apache Artemis JMS queue.
 * The JmsTemplate and queue name are supplied by {@link ArtemisAuditAppenderHolder}
 * once the Spring context is ready.
 */
public class ArtemisAuditAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        ArtemisAuditAppenderHolder.SendFunction sender = ArtemisAuditAppenderHolder.getSend();
        if (sender == null) {
            return;
        }
        String message = event.getFormattedMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        try {
            sender.send(message);
        } catch (Exception e) {
            addError("Failed to send audit log to Artemis: " + e.getMessage(), e);
        }
    }
}
