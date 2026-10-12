package uk.gov.hmcts.amp.entraapp.service;

import com.microsoft.graph.applications.item.addpassword.AddPasswordPostRequestBody;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.PasswordCredential;
import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.amp.entraapp.config.EntraProperties;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

/**
 * Registers a Microsoft Entra application and hands back credentials a subscriber can use. Three Graph
 * calls, in this order:
 *   applications().post(..)                       the registration. Carries id (object id) and appId (client id)
 *   servicePrincipals().post(..)                  the identity in the tenant, so we can get a token
 *   applications().byApplicationId(id).addPassword()   the secret, returned once and never again
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntraAppRegistrationService {

    public record Registration(String objectId, String clientId, String clientSecret, String keyId) {
    }

    public record Summary(String objectId, String clientId, String displayName, OffsetDateTime created) {
    }

    private interface GraphCall<T> {
        T call();
    }

    private final GraphServiceClient graph;
    private final EntraProperties config;


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

    public List<Summary> list() {
        List<Summary> found = new ArrayList<>();
        ApplicationCollectionResponse page = graph.applications().get(request ->
                request.queryParameters.select = new String[] {"id", "appId", "displayName", "createdDateTime"});
        while (page != null) {
            page.getValue().forEach(application -> found.add(new Summary(application.getId(),
                    application.getAppId(), application.getDisplayName(), application.getCreatedDateTime())));
            String nextPage = page.getOdataNextLink();
            page = nextPage == null ? null
                    : graph.applications().withUrl(nextPage).get();
        }
        found.sort(comparing(Summary::created, nullsLast(naturalOrder())));
        return found;
    }

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

    private ServicePrincipal createServicePrincipal(final String clientId) {
        ServicePrincipal servicePrincipal = new ServicePrincipal();
        servicePrincipal.setAppId(clientId);
        return graph.servicePrincipals().post(servicePrincipal);
    }

    private PasswordCredential addPassword(final String objectId) {
        PasswordCredential wanted = new PasswordCredential();
        wanted.setDisplayName("entra-app-registration-demo");
        AddPasswordPostRequestBody request = new AddPasswordPostRequestBody();
        request.setPasswordCredential(wanted);
        return graph.applications().byApplicationId(objectId).addPassword().post(request);
    }

    private <T> T withRetry(final String description, final GraphCall<T> call) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            try {
                return call.call();
            } catch (final RuntimeException e) {
                lastFailure = e;
                log.warn("Graph call '{}' failed on attempt {}/{}: {}", description, attempt, config.maxAttempts(),
                        e.getMessage());
                if (attempt < config.maxAttempts()) {
                    pause();
                }
            }
        }
        log.error("Graph call '{}' did not succeed after {} attempts.", description, config.maxAttempts(), lastFailure);
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Could not register the application. Please try again.", lastFailure);
    }

    private void pause() {
        try {
            Thread.sleep(config.retryDelayMillis());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Interrupted while retrying.");
        }
    }
}
