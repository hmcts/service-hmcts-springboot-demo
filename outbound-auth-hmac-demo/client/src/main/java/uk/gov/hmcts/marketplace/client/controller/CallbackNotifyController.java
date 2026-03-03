package uk.gov.hmcts.marketplace.client.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.client.service.HmacVerificationService;
import uk.gov.hmcts.marketplace.client.service.SecretStore;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CallbackNotifyController {

    private final SecretStore secretStore;
    private final HmacVerificationService hmacVerificationService;

    @PostMapping("/callback-notify")
    public ResponseEntity<Void> callbackNotify(
            @RequestHeader(value = "X-HMAC-Signature", required = false) String signature,
            @RequestHeader(value = "X-HMAC-Key-Id", required = false) String keyId,
            @RequestBody(required = false) String payload) {
        if (signature == null || keyId == null) {
            log.warn("Missing HMAC headers");
            return ResponseEntity.status(401).build();
        }
        String secret = secretStore.get(keyId);
        if (secret == null) {
            log.warn("Oops... No secret found for keyId: {}", keyId);
            return ResponseEntity.status(401).build();
        }
        String body = payload != null ? payload : "";
        if (!hmacVerificationService.verify("POST", "/callback-notify", body, signature, secret)) {
            log.warn("HMAC verification failed");
            return ResponseEntity.status(401).build();
        }
        log.info("Received notification: {}", payload);
        return ResponseEntity.ok().build();
    }
}
