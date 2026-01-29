package uk.gov.hmcts.marketplace.client.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.client.exception.SubscriptionConflictException;
import uk.gov.hmcts.marketplace.client.model.ErrorResponse;
import uk.gov.hmcts.marketplace.client.model.RotateSecretResponse;
import uk.gov.hmcts.marketplace.client.model.SecretResponse;
import uk.gov.hmcts.marketplace.client.service.SubscribeService;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SubscribeController {

    private final SubscribeService subscribeService;

    @PostMapping("/subscribe")
    public ResponseEntity<SecretResponse> subscribe(@RequestParam String name) {
        log.info("Subscribing with name: {}", name);
        SecretResponse payload = subscribeService.subscribe(name);
        return ResponseEntity.accepted().body(payload);
    }

    @ExceptionHandler(SubscriptionConflictException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionConflict(SubscriptionConflictException e) {
        return ResponseEntity
                .status(409)
                .body(new ErrorResponse("Conflict", e.getMessage()));
    }

    /**
     * Rotate the secret for keyId: client calls the server to rotate, then updates its local store
     * so future notifications (callback-notify) are verified with the new secret.
     */
    @PostMapping("/rotate-secret")
    public ResponseEntity<RotateSecretResponse> rotateSecret(@RequestParam String keyId) {
        log.info("Rotating secret for keyId: {}", keyId);
        subscribeService.rotateSecret(keyId);
        return ResponseEntity.ok(new RotateSecretResponse(keyId, "Secret rotated and stored for future notifications"));
    }
}

