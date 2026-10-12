package uk.gov.hmcts.amp.entraapp.service;

import com.microsoft.graph.applications.item.addpassword.AddPasswordPostRequestBody;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.PasswordCredential;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * Registers a Microsoft Entra application and hands back credentials a subscriber can use. Three Graph
 * calls, in this order:
 *
 * <pre>
 *   applications().post(..)                       the registration. Carries id (object id) and appId (client id)
 *   servicePrincipals().post(..)                  the identity in the tenant, without which the credentials
 *                                                 cannot get a token at all
 *   applications().byApplicationId(id).addPassword()   the secret, returned once and never again
 * </pre>
 *
 * <p>The last two are retried. Graph's directory read replicas lag the write that created the
 * application, so the very next call can fail against a stale replica. Nothing below the wire shows
 * this: it appears only against the real Graph.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntraAppRegistrationClient {

    private static final int MAX_ATTEMPTS = 4;

    /**
     * {@code objectId} is the directory object, which is how the application is addressed again later;
     * {@code clientId} is what a subscriber authenticates with. Both are returned because the caller
     * needs one to use the application and the other to remove it.
     */
    public record Registration(String objectId, String clientId, String clientSecret, String keyId) {
    }

    private interface GraphCall<T> {
        T call();
    }

    private final GraphServiceClient graph;

    // Not final, so a test can shorten it. Everything else waits the full two seconds.
    private Duration retryDelay = Duration.ofSeconds(2);

    public Registration register(final String applicationName) {
        Application created = createApplication(applicationName);
        String objectId = created.getId();
        String clientId = created.getAppId();

        // From here the application exists. Anything that goes wrong now has to take it away again, or
        // it is left in the directory with no service principal, no secret, and nobody holding its id.
        try {
            withRetry("create service principal", () -> createServicePrincipal(clientId));
            PasswordCredential secret = withRetry("add password", () -> addPassword(objectId));

            log.info("Registered Entra application {} for '{}'", clientId, applicationName);
            return new Registration(objectId, clientId, secret.getSecretText(), String.valueOf(secret.getKeyId()));
        } catch (final RuntimeException registrationFailed) {
            removeOrphan(objectId, registrationFailed);
            throw registrationFailed;
        }
    }

    /**
     * Removes an application whose registration did not finish. A failure to clean up must not replace
     * the failure that caused it - whoever reads the error needs to know registration failed, not that
     * tidying up afterwards did.
     */
    private void removeOrphan(final String objectId, final RuntimeException cause) {
        try {
            delete(objectId);
            log.warn("Registration failed after the application was created; removed orphaned {}", objectId);
        } catch (final RuntimeException cleanupFailed) {
            cause.addSuppressed(cleanupFailed);
            log.error("Registration failed and orphaned application {} could not be removed.",
                    objectId, cleanupFailed);
        }
    }

    /**
     * Removes an application, so a registration that failed part-way does not leave one behind whose
     * secret nobody was ever shown. Deleting it removes its service principal too.
     */
    public void delete(final String objectId) {
        withRetry("delete application", () -> {
            graph.applications().byApplicationId(objectId).delete();
            return null;
        });
        log.info("Deleted Entra application {}", objectId);
    }

    private Application createApplication(final String applicationName) {
        Application application = new Application();
        application.setDisplayName(applicationName);
        return graph.applications().post(application);
    }

    // Built from the client id, not the object id: a service principal names the application it is for
    // by appId.
    private ServicePrincipal createServicePrincipal(final String clientId) {
        ServicePrincipal servicePrincipal = new ServicePrincipal();
        servicePrincipal.setAppId(clientId);
        return graph.servicePrincipals().post(servicePrincipal);
    }

    // Addressed by object id, which only the create response carries.
    private PasswordCredential addPassword(final String objectId) {
        PasswordCredential wanted = new PasswordCredential();
        wanted.setDisplayName("entra-app-registration-demo");
        AddPasswordPostRequestBody request = new AddPasswordPostRequestBody();
        request.setPasswordCredential(wanted);
        return graph.applications().byApplicationId(objectId).addPassword().post(request);
    }

    private <T> T withRetry(final String description, final GraphCall<T> call) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return call.call();
            } catch (final RuntimeException e) {
                lastFailure = e;
                log.warn("Graph call '{}' failed on attempt {}/{}: {}", description, attempt, MAX_ATTEMPTS,
                        e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    pause();
                }
            }
        }
        log.error("Graph call '{}' did not succeed after {} attempts.", description, MAX_ATTEMPTS, lastFailure);
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Could not register the application. Please try again.", lastFailure);
    }

    private void pause() {
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (final InterruptedException e) {
            // Restore the flag rather than swallow it, so a shutting-down pod still sees the interrupt.
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Interrupted while retrying.");
        }
    }

    void retryDelay(final Duration delay) {
        this.retryDelay = delay;
    }
}
