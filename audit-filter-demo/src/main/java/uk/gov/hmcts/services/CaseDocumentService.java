package uk.gov.hmcts.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.config.AppProperties;
import uk.gov.hmcts.cp.audit.model.AuditMdcKeys;
import uk.gov.hmcts.cp.audit.service.AuditUuidService;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class CaseDocumentService {

    AppProperties appProperties;
    AuditUuidService auditUuidService;

    public void getDocument(final UUID documentId) {
        // We mimic a service that pulls the userId from an environment variable such as CJSCPPUID
        MDC.put(AuditMdcKeys.USER_ID, appProperties.getCjscppuid().toString());

        // We mimic a service that translate the external documentId to an internal materialId
        // And add the materiaLid to MDC where it will be added to the response Audit payload
        UUID materialId = auditUuidService.randomUUID();
        log.info("Using materialId {}", materialId);
        MDC.put(AuditMdcKeys.MATERIAL_ID, materialId.toString());
    }
}
