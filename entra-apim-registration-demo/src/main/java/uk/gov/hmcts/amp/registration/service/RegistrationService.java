package uk.gov.hmcts.amp.registration.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.amp.registration.apim.ApimClient;
import uk.gov.hmcts.amp.registration.config.RegistrationProperties;
import uk.gov.hmcts.amp.registration.entra.EntraGraphClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The journey: register an application (Entra gives it a Client ID and a Client Secret), connect it to an API
 * (APIM gives it a Subscription Key for that API), and take either away (Entra and APIM first, then forget it).
 *
 * <p>The order is the same everywhere: refuse what can be refused, then ask Entra and APIM, then remember the
 * result. Nothing is stored, so there is nothing to roll back; a failed call just leaves nothing behind.
 * The Client Secret is returned once, to the caller, and not kept.
 */
@Service
public class RegistrationService {

    public static final int MAX_NAME = 100;

    public record Created(RegisteredApplication application, String clientSecret) {
    }

    private final EntraGraphClient entra;
    private final ApimClient apim;
    private final Map<String, String> products;
    private final Map<UUID, RegisteredApplication> applications = new ConcurrentHashMap<>();

    public RegistrationService(final EntraGraphClient entra, final ApimClient apim,
                               final RegistrationProperties properties) {
        this.entra = entra;
        this.apim = apim;
        this.products = properties.apim().products();
    }

    /** The APIs on offer. */
    public List<String> apis() {
        return products.keySet().stream().sorted().toList();
    }

    public List<RegisteredApplication> list(final String owner) {
        return applications.values().stream()
            .filter(application -> application.owner().equals(owner))
            .sorted((a, b) -> a.createdAt().compareTo(b.createdAt()))
            .toList();
    }

    public Created create(final String owner, final String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Enter an application name of " + MAX_NAME + " characters or fewer.");
        }
        EntraGraphClient.Registration registration = entra.register(trimmed);
        RegisteredApplication application = new RegisteredApplication(owner, trimmed, registration.clientId());
        applications.put(application.id(), application);
        return new Created(application, registration.clientSecret());
    }

    public RegisteredApplication connect(final String owner, final String applicationId, final String apiId) {
        RegisteredApplication application = owned(owner, applicationId);
        String productId = Optional.ofNullable(apiId).map(products::get)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "That API is not available."));
        if (application.connections().stream().anyMatch(c -> c.apiId().equals(apiId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That API is already connected.");
        }
        ApimClient.Subscription subscription = apim.subscribe(application.name(), productId);
        application.connections().add(new RegisteredApplication.Connection(apiId, subscription));
        return application;
    }

    public RegisteredApplication disconnect(final String owner, final String applicationId, final String apiId) {
        RegisteredApplication application = owned(owner, applicationId);
        application.connections().stream().filter(c -> c.apiId().equals(apiId)).findFirst().ifPresent(connection -> {
            // The subscription goes first: if APIM refuses, the key is still shown and this can be tried again.
            apim.unsubscribe(connection.subscription().subscriptionName());
            application.connections().remove(connection);
        });
        return application;
    }

    public void delete(final String owner, final String applicationId) {
        RegisteredApplication application = owned(owner, applicationId);
        for (RegisteredApplication.Connection connection : application.connections()) {
            apim.unsubscribe(connection.subscription().subscriptionName());
        }
        entra.delete(application.clientId());
        applications.remove(application.id());
    }

    // Someone else's application, or one that is not there, gets the same answer: not found.
    private RegisteredApplication owned(final String owner, final String applicationId) {
        try {
            RegisteredApplication application = applications.get(UUID.fromString(applicationId));
            if (application != null && application.owner().equals(owner)) {
                return application;
            }
        } catch (IllegalArgumentException e) {
            // not a UUID: falls through to not found
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found.");
    }
}
