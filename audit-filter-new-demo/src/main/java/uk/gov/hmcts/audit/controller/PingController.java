package uk.gov.hmcts.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.audit.annotation.AuditDetail;
import uk.gov.hmcts.audit.annotation.AuditExclude;
import uk.gov.hmcts.audit.service.DocumentService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PingController {

    private final DocumentService documentService;

    @GetMapping("/client-subscriptions/{clientSubscriptionId}/documents/{documentId}")
    @AuditDetail(
        eventName  = "hearing-results-document.get-document",
        action     = "Download",
        pathParams = {"clientSubscriptionId", "documentId"}
    )
    public ResponseEntity<Void> valid(
            @PathVariable final UUID clientSubscriptionId,
            @PathVariable final UUID documentId) {
        documentService.resolveMaterialId(documentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/excluded")
    @AuditExclude
    public ResponseEntity<Void> excluded() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/noannotation")
    public ResponseEntity<Void> noAnnotation() {
        return ResponseEntity.ok().build();
    }
}
