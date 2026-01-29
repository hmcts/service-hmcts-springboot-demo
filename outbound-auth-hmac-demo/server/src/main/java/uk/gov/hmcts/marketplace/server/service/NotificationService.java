package uk.gov.hmcts.marketplace.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.marketplace.server.model.SubscriberRow;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 200;

    private final RestTemplate restTemplate;
    private final SubscriberStore subscriberStore;
    private final HmacSigningService hmacSigningService;

    public void notifyClient(String callbackUrl, String name, String keyId) {
        String payload = name + ", you have a news";
        log.info("Notifying client at {} with payload: {}", callbackUrl, payload);

        String secret = subscriberStore.getSecretByKeyId(keyId);
        if (secret == null) {
            throw new IllegalArgumentException("No secret found for keyId: " + keyId);
        }

        String path = "/callback-notify";
        String signature = hmacSigningService.sign("POST", path, payload, secret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.add("X-HMAC-Signature", signature);
        headers.add("X-HMAC-Key-Id", keyId);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restTemplate.postForEntity(callbackUrl, entity, Void.class);
                return;
            } catch (HttpClientErrorException.Unauthorized e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Callback still returned 401 after {} attempts", MAX_ATTEMPTS);
                    throw e;
                }
                log.debug("Callback returned 401 (attempt {}/{}), retrying in {}ms", attempt, MAX_ATTEMPTS, RETRY_DELAY_MS);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while retrying callback", ie);
                }
            }
        }
    }

    /**
     * Sends a notification to one subscriber with a custom message, signed with that subscriber's secret.
     */
    public void notifySubscriber(SubscriberRow row, String message) {
        String callbackUrl = row.getCallbackUrl();
        String keyId = row.getKeyId();
        String secret = row.getSecret();
        String payload = message != null ? message : "";
        log.info("Notifying subscriber {} at {} with message: {}", row.getName(), callbackUrl, payload);

        String path = "/callback-notify";
        String signature = hmacSigningService.sign("POST", path, payload, secret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.add("X-HMAC-Signature", signature);
        headers.add("X-HMAC-Key-Id", keyId);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restTemplate.postForEntity(callbackUrl, entity, Void.class);
                return;
            } catch (HttpClientErrorException.Unauthorized e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Callback still returned 401 after {} attempts for {}", MAX_ATTEMPTS, row.getName());
                    throw e;
                }
                log.debug("Callback returned 401 (attempt {}/{}), retrying in {}ms", attempt, MAX_ATTEMPTS, RETRY_DELAY_MS);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while retrying callback", ie);
                }
            }
        }
    }
}
