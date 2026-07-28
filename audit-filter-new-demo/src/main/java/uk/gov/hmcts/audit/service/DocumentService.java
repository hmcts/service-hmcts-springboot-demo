package uk.gov.hmcts.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.audit.model.AuditMdcKeys;

import java.util.UUID;

/**
 * Demo service simulating an internal lookup of a public documentId to a private materialId.
 * In production this would query the database; the key point is the MDC.put which
 * makes the resolved materialId available to the audit filter on the response leg.
 */
@Service
@Slf4j
public class DocumentService {

    public UUID resolveMaterialId(final UUID documentId) {
        final UUID materialId = deriveForDemo(documentId);
        MDC.put(AuditMdcKeys.MATERIAL_ID, materialId.toString());
        log.debug("Resolved documentId={} to materialId={}", documentId, materialId);
        return materialId;
    }

    private UUID deriveForDemo(final UUID documentId) {
        return UUID.nameUUIDFromBytes(documentId.toString().getBytes());
    }
}
