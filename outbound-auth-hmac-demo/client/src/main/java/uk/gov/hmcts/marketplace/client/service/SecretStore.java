package uk.gov.hmcts.marketplace.client.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SecretStore {

    private final Map<String, String> keyIdToSecret = new ConcurrentHashMap<>();

    public void put(String keyId, String secret) {
        keyIdToSecret.put(keyId, secret);
    }

    public String get(String keyId) {
        return keyIdToSecret.get(keyId);
    }
}
