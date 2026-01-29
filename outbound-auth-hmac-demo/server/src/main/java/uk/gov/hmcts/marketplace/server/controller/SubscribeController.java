package uk.gov.hmcts.marketplace.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.server.model.SecretResponse;
import uk.gov.hmcts.marketplace.server.model.SubscribeRequest;
import uk.gov.hmcts.marketplace.server.model.SubscriberRow;
import uk.gov.hmcts.marketplace.server.service.NotificationService;
import uk.gov.hmcts.marketplace.server.service.SubscriberStore;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscribeController {

    private final SubscriberStore subscriberStore;
    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${notify.callback-delay-ms:500}")
    private long callbackDelayMs;

    @PostMapping("/subscribe")
    public ResponseEntity<SecretResponse> subscribe(@RequestBody SubscribeRequest request) {
        String name = request.getName();
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String keyId;
        String secret;
        String callbackUrl = request.getCallbackUrl();
        if (request.getKeyId() != null && (secret = subscriberStore.getSecretByKeyId(request.getKeyId())) != null) {
            keyId = request.getKeyId();
            log.info("Subscribe: name={}, callbackUrl={}, using existing keyId={}", name, callbackUrl, keyId);
        } else {
            keyId = UUID.randomUUID().toString();
            secret = UUID.randomUUID().toString().replace("-", "");
            log.info("Subscribe: name={}, callbackUrl={}, generated keyId={}", name, callbackUrl, keyId);
        }

        SubscriberRow row = new SubscriberRow(name, keyId, secret, callbackUrl);
        if (!subscriberStore.register(row)) {
            log.warn("Duplicate subscription for name: {}", name);
            return ResponseEntity.status(409).build();
        }

        long delayMs = this.callbackDelayMs;
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted before notifying client", e);
            }
            notificationService.notifyClient(callbackUrl, name, keyId);
        });

        return ResponseEntity.accepted().body(new SecretResponse(keyId, secret));
    }
}
