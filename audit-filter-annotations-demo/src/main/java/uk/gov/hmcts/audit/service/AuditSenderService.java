package uk.gov.hmcts.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sends a generated audit payload to the audit backend.
 */
@Service
@Slf4j
public class AuditSenderService {

    public void send(final String payload) {
        log.info("[AUDIT] sending payload={}", payload);
    }
}
