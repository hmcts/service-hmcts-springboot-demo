package uk.gov.hmcts.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.audit.annotation.AuditDetail;
import uk.gov.hmcts.cp.audit.model.AuditMdcKeys;
import uk.gov.hmcts.services.CaseDocumentService;

import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
public class RootController {

    private final CaseDocumentService caseDocumentService;

    @GetMapping("/cases/{caseId}/documents/{documentId}")
    @AuditDetail(
        origin     = "case-documents",
        component  = "DOCUMENT_API",
        eventName  = "case-documents.get-document",
        action     = "Download",
        pathParams        = {"caseId", "documentId"},
        expectedMdcFields = {AuditMdcKeys.MATERIAL_ID, AuditMdcKeys.USER_ID}
    )
    public ResponseEntity<Map<String, UUID>> getCaseDocument(
            @PathVariable final UUID caseId,
            @PathVariable final UUID documentId) {
        caseDocumentService.getDocument(documentId);
        return ResponseEntity.ok(Map.of("caseId", caseId, "documentId", documentId));
    }
}
