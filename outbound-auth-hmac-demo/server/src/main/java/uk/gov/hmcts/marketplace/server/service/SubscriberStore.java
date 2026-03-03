package uk.gov.hmcts.marketplace.server.service;

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
 * Orphan secrets (from GET /api/secret with no subscriber) are in a separate keyId→secret map.
 */
@Component
public class SubscriberStore {

    /** Table: name (primary key) → SubscriberRow */
    private final Map<String, SubscriberRow> table = new ConcurrentHashMap<>();

    /** Index: keyId → name (for lookup by keyId) */
    private final Map<String, String> indexByKeyId = new ConcurrentHashMap<>();

    /** Orphan secrets: keyId → secret (keys from GET /api/secret with no subscriber row) */
    private final Map<String, String> orphanSecrets = new ConcurrentHashMap<>();

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
     * Lookup secret by keyId. Subscriber rows first, then orphan secrets.
     */
    public String getSecretByKeyId(String keyId) {
        SubscriberRow row = getByKeyId(keyId);
        if (row != null) {
            return row.getSecret();
        }
        return orphanSecrets.get(keyId);
    }

    // --- Insert / update ---

    /**
     * Insert a row. Name is the primary key.
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
            return true;
        }
        return false;
    }

    /**
     * Store a secret for a keyId with no subscriber (e.g. from GET /api/secret).
     */
    public void putSecret(String keyId, String secret) {
        orphanSecrets.put(keyId, secret);
    }

    /**
     * Replace the secret for a keyId. Updates the row if present, otherwise the orphan map.
     */
    public void replaceSecret(String keyId, String newSecret) {
        SubscriberRow row = getByKeyId(keyId);
        if (row != null) {
            row.setSecret(newSecret);
        } else {
            orphanSecrets.put(keyId, newSecret);
        }
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
