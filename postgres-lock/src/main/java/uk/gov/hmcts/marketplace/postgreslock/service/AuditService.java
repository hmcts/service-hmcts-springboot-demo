package uk.gov.hmcts.marketplace.postgreslock.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.marketplace.postgreslock.client.AuditClient;
import uk.gov.hmcts.marketplace.postgreslock.entity.AuditEntity;
import uk.gov.hmcts.marketplace.postgreslock.repository.AuditRepository;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;
    private final AuditClient auditClient;

    @Transactional
    public void sendAudits() {
        while (true) {
            Optional<AuditEntity> next = auditRepository.findNextAudit();
            if (next.isPresent()) {
                auditClient.sendAudit(next.get().getPayload());
                auditRepository.delete(next.get());
            } else {
                return;
            }
        }
    }
}
