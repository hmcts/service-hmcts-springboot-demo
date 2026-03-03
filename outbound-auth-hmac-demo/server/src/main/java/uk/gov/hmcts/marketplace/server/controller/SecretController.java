package uk.gov.hmcts.marketplace.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.server.model.RotateSecretRequest;
import uk.gov.hmcts.marketplace.server.model.SecretResponse;
import uk.gov.hmcts.marketplace.server.service.SubscriberStore;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SecretController {

    private final SubscriberStore subscriberStore;

    @PostMapping("/secret/rotate")
    public ResponseEntity<SecretResponse> rotateSecret(@RequestBody RotateSecretRequest request) {
        String keyId = request.getKeyId();
        if (keyId == null || keyId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String existing = subscriberStore.getSecretByKeyId(keyId);
        if (existing == null) {
            log.warn("Rotate requested for unknown keyId: {}", keyId);
            return ResponseEntity.notFound().build();
        }
        String newSecret = UUID.randomUUID().toString().replace("-", "");
        subscriberStore.replaceSecret(keyId, newSecret);
        log.info("Rotated secret for keyId: {}", keyId);
        return ResponseEntity.ok(new SecretResponse(keyId, newSecret));
    }
}
