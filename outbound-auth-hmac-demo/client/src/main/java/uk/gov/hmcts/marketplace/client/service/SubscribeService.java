package uk.gov.hmcts.marketplace.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.marketplace.client.exception.SubscriptionConflictException;
import uk.gov.hmcts.marketplace.client.model.SecretResponse;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeService {

    private final RestTemplate restTemplate;
    private final SecretStore secretStore;

    @Value("${client.server-base-url:http://localhost:8080}")
    private String serverBaseUrl;

    @Value("${client.callback-url:http://localhost:8081/callback-notify}")
    private String callbackUrl;

    /**
     * Subscribe with the given name. Client sends name and callbackUrl to server; server generates keyId and secret,
     * returns them in the response. Client stores in local store only. Returns keyId and secret for the caller.
     * If secret is ever lost, use rotate-secret to get a new one from the server.
     */
    public SecretResponse subscribe(String name) {
        String url = serverBaseUrl + "/api/subscribe";
        Map<String, String> body = Map.of(
                "name", name,
                "callbackUrl", callbackUrl
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<SecretResponse> response;
        try {
            response = restTemplate.postForEntity(url, entity, SecretResponse.class);
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Server returned 409 Conflict for name: {}", name);
            throw new SubscriptionConflictException("Subscription already exists for name: " + name);
        }

        SecretResponse secretResponse = response.getBody();
        if (secretResponse == null) {
            throw new IllegalStateException("Failed to subscribe: no keyId/secret in response");
        }
        secretStore.put(secretResponse.getKeyId(), secretResponse.getSecret());
        log.info("Subscribed; stored secret for keyId: {} (from client store only)", secretResponse.getKeyId());
        return secretResponse;
    }

    /**
     * Rotate the secret for the given keyId. The client calls the server to rotate; the server
     * replaces the old secret with a new one and returns it. The client stores the new secret
     * in its local store so it can verify future notifications (callback-notify) with the new secret.
     */
    public void rotateSecret(String keyId) {
        String url = serverBaseUrl + "/api/secret/rotate";
        Map<String, String> body = Map.of("keyId", keyId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        SecretResponse response = restTemplate.postForEntity(url, entity, SecretResponse.class).getBody();
        if (response == null) {
            throw new IllegalStateException("Failed to rotate secret: no response from server");
        }
        secretStore.put(response.getKeyId(), response.getSecret());
        log.info("Rotated and stored new secret for keyId: {} (will be used for future notifications)", response.getKeyId());
    }
}
