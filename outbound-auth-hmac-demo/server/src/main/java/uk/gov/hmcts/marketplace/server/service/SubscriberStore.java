package uk.gov.hmcts.marketplace.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.marketplace.server.model.SubscriberRow;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store that holds a collection of {@link SubscriberRow}, like a database table.
 * <p>
 * Table: primary key = name; each row has name, keyId, secret, callbackUrl.
 * Index: keyId → name for lookup by keyId.
 * <p>
 * Secrets are persisted to and read from Azure Key Vault via KeyVaultSecretStore
 * The in-memory maps only hold subscriber metadata (name, callbackUrl, keyId).
 */
@Component
@RequiredArgsConstructor
public class SubscriberStore {

    /** Table: name (primary key) → SubscriberRow */
    private final Map<String, SubscriberRow> table = new ConcurrentHashMap<>();

    /** Index: keyId → name (for lookup by keyId) */
    private final Map<String, String> indexByKeyId = new ConcurrentHashMap<>();

    /** Source of truth for HMAC secrets – backed by Azure Key Vault. */
    private final KeyVaultSecretStore keyVaultSecretStore;

    // --- Lookup by name (primary key) ---

    /**
     * Lookup row by subscriber name.
     */
    public SubscriberRow getByName(String name) {
        return table.get(name);
    }

    /**
     * Lookup row by keyId (via index).
     */
    public SubscriberRow getByKeyId(String keyId) {
        String name = indexByKeyId.get(keyId);
        return name != null ? table.get(name) : null;
    }

    /**
     * Lookup secret by keyId. Reads directly from Azure Key Vault.
     */
    public String getSecretByKeyId(String keyId) {
        return keyVaultSecretStore.get(keyId);
    }

    // --- Insert / update ---

    /**
     * Insert a row. Name is the primary key.
     * The row's secret is persisted to Azure Key Vault.
     *
     * @return true if inserted, false if name already exists (duplicate)
     */
    public boolean register(SubscriberRow row) {
        String name = row.getName();
        if (name == null || name.isBlank()) {
            return false;
        }
        if (table.putIfAbsent(name, row) == null) {
            indexByKeyId.put(row.getKeyId(), name);
            keyVaultSecretStore.put(row.getKeyId(), row.getSecret());
            return true;
        }
        return false;
    }

    /**
     * Store a secret for a keyId with no subscriber (e.g. from the seed / GET /api/secret).
     * Persists to Azure Key Vault.
     */
    public void putSecret(String keyId, String secret) {
        keyVaultSecretStore.put(keyId, secret);
    }

    /**
     * Replace the secret for a keyId.
     * Updates Key Vault and keeps the local row cache consistent.
     */
    public void replaceSecret(String keyId, String newSecret) {
        SubscriberRow row = getByKeyId(keyId);
        if (row != null) {
            row.setSecret(newSecret);
        }
        keyVaultSecretStore.put(keyId, newSecret);
    }

    /**
     * Whether a subscriber name exists.
     */
    public boolean isRegistered(String name) {
        return table.containsKey(name);
    }

    /**
     * All subscriber rows (read-only view of the table).
     */
    public Collection<SubscriberRow> getAllRows() {
        return Collections.unmodifiableCollection(table.values());
    }
}