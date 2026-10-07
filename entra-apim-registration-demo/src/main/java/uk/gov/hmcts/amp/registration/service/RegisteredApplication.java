package uk.gov.hmcts.amp.registration.service;

import uk.gov.hmcts.amp.registration.apim.ApimClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** One registered application, kept in memory: who owns it, its real Client ID, and the keys it was given. */
public class RegisteredApplication {

    private final UUID id = UUID.randomUUID();
    private final String owner;
    private final String name;
    private final String clientId;
    private final Instant createdAt = Instant.now();
    // apiId -> the subscription issued for it
    private final List<Connection> connections = new CopyOnWriteArrayList<>();

    public record Connection(String apiId, ApimClient.Subscription subscription) {
    }

    public RegisteredApplication(final String owner, final String name, final String clientId) {
        this.owner = owner;
        this.name = name;
        this.clientId = clientId;
    }

    public UUID id() {
        return id;
    }

    public String owner() {
        return owner;
    }

    public String name() {
        return name;
    }

    public String clientId() {
        return clientId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<Connection> connections() {
        return connections;
    }
}
