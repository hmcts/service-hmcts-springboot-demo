package uk.gov.hmcts.marketplace.client.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.client.model.SecretResponse;
import uk.gov.hmcts.marketplace.client.service.SecretStore;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SecretController {

    private final SecretStore secretStore;

    /**
     * Get secret from the client's local store only. No server call.
     * If the secret was lost, use POST /rotate-secret?keyId=... to get a new one from the server.
     */
    @GetMapping("/secret")
    public ResponseEntity<SecretResponse> getSecret(@RequestParam String keyId) {
        String secret = secretStore.get(keyId);
        if (secret == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new SecretResponse(keyId, secret));
    }
}
