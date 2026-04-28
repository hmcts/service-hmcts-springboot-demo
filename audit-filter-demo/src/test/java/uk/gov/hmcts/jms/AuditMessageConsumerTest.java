package uk.gov.hmcts.jms;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@Slf4j
class AuditMessageConsumerTest {

    @InjectMocks
    AuditMessageConsumer auditMessageConsumer;

    @Test
    void audit_jms_message_should_simply_make_log_message() {
        final ByteArrayOutputStream capturedStdOut = captureStdOut();
        auditMessageConsumer.on("msg");
        String auditMessage = capturedStdOut.toString();
        log.info("AuditMessage:{}", auditMessage);
        assertThat(auditMessage).endsWith("Audit payload: msg\n");
    }

    private ByteArrayOutputStream captureStdOut() {
        final ByteArrayOutputStream capturedStdOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedStdOut, true, StandardCharsets.UTF_8));
        return capturedStdOut;
    }
}