package uk.gov.hmcts.marketplace.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.marketplace.filters.TracingFilter.CORRELATION_ID_KEY;

@Slf4j
@SpringBootTest
class LoggingIntegrationTest {

    private final PrintStream originalStdOut = System.out;

    @AfterEach
    void tearDown() {
        System.setOut(originalStdOut);
        MDC.clear();
    }

    @Test
    void log_output_should_contain_correlation_id_from_mdc() throws Exception {
        final String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_KEY, correlationId);

        final ByteArrayOutputStream captured = captureStdOut();
        log.info("test log message");

        final Map<String, Object> logFields = new ObjectMapper()
                .readValue(captured.toString(StandardCharsets.UTF_8), new TypeReference<>() {});

        assertThat(logFields.get(CORRELATION_ID_KEY)).isEqualTo(correlationId);
        assertThat(logFields.get("message")).isEqualTo("test log message");
        assertThat(logFields.get("level")).isEqualTo("INFO");
    }

    private ByteArrayOutputStream captureStdOut() {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        return out;
    }
}
