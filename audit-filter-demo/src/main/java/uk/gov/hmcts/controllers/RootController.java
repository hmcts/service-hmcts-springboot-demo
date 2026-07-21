package uk.gov.hmcts.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.audit.annotation.AuditDetail;

import java.util.Map;
import java.util.UUID;

@RestController
public class RootController {

    @GetMapping("/cases/{caseId}/documents/{documentId}")
    @AuditDetail(
        origin     = "case-documents",
        component  = "DOCUMENT_API",
        eventName  = "case-documents.get-document",
        action     = "Download",
        pathParams = {"caseId", "documentId"}
    )
    public ResponseEntity<Map<String, UUID>> getCaseDocument(
            @PathVariable final UUID caseId,
            @PathVariable final UUID documentId) {
        return ResponseEntity.ok(Map.of("caseId", caseId, "documentId", documentId));
    }
}
