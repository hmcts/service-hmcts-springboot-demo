package uk.gov.hmcts.amp.entraapp.service;

import com.microsoft.graph.applications.item.addpassword.AddPasswordPostRequestBody;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.PasswordCredential;
import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignmentCollectionResponse;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.ApplicationCollectionResponse;
import com.microsoft.graph.models.ServicePrincipalCollectionResponse;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.amp.entraapp.config.EntraProperties;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public record Detail(String objectId, String clientId, String displayName, OffsetDateTime created,
                         List<String> roles) {
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
            deleteByObjectId(objectId);
            log.warn("Registration failed after the application was created; removed orphaned {}", objectId);
        } catch (final RuntimeException cleanupFailed) {
            cause.addSuppressed(cleanupFailed);
            log.error("Registration failed and orphaned application {} could not be removed.",
                    objectId, cleanupFailed);
        }
    }

    /**
     * Lets a registered application call one of the platform's APIs, by assigning it an app role on
     * the API's service principal. Nothing in APIM changes: its validate-jwt policy already requires
     * the role, and the consumer's token starts carrying it once this is done.
     *
     * <p>Needs the Graph permission {@code AppRoleAssignment.ReadWrite.All}, which no principal in
     * hmctsextsbox holds today - so this fails with a message naming it until consent is granted.
     */
    public void grantApiAccess(final String consumerClientId, final String roleValue) {
        String consumerPrincipalId = servicePrincipalObjectId(consumerClientId,
                "No service principal for application " + consumerClientId);
        String apiPrincipalId = servicePrincipalObjectId(config.apiClientId(),
                "No service principal for the API " + config.apiClientId());

        AppRoleAssignment assignment = new AppRoleAssignment();
        assignment.setPrincipalId(UUID.fromString(consumerPrincipalId));
        assignment.setResourceId(UUID.fromString(apiPrincipalId));
        assignment.setAppRoleId(appRoleId(apiPrincipalId, roleValue));

        try {
            graph.servicePrincipals().byServicePrincipalId(apiPrincipalId)
                    .appRoleAssignedTo().post(assignment);
        } catch (final ApiException e) {
            throw assignmentRefused(e);
        }
        log.info("Granted {} on {} to application {}", roleValue, config.apiClientId(), consumerClientId);
    }

    private String servicePrincipalObjectId(final String appId, final String notFound) {
        ServicePrincipalCollectionResponse found = graph.servicePrincipals().get(request ->
                request.queryParameters.filter = "appId eq '" + appId + "'");
        if (found == null || found.getValue() == null || found.getValue().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, notFound);
        }
        return found.getValue().getFirst().getId();
    }

    private UUID appRoleId(final String apiPrincipalId, final String roleValue) {
        List<AppRole> roles = graph.servicePrincipals().byServicePrincipalId(apiPrincipalId).get().getAppRoles();
        return roles.stream()
                .filter(role -> roleValue.equals(role.getValue()))
                .map(AppRole::getId)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The API offers no role '" + roleValue + "'. It has: "
                                + roles.stream().map(AppRole::getValue).toList()));
    }

    /** The permission nobody in the tenant holds yet, named so the error says what to ask for. */
    private ResponseStatusException assignmentRefused(final ApiException cause) {
        // Logged here because this path is not retried, so nothing else would record why Graph said no.
        log.error("Graph refused the app role assignment with {}: {}",
                cause.getResponseStatusCode(), cause.getMessage(), cause);
        if (cause.getResponseStatusCode() == HttpStatus.FORBIDDEN.value()) {
            // 500, not 503: nothing about this is temporary, and a 503 invites a client to retry
            // forever against something only a consent grant will fix.
            return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Graph refused the app role assignment. The onboarding identity needs the "
                            + "AppRoleAssignment.ReadWrite.All application permission.", cause);
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not assign the app role.", cause);
    }

    /** One application, with the API roles it has been granted - what it can actually call. */
    public Detail get(final String clientId) {
        Summary summary = list().stream()
                .filter(application -> clientId.equals(application.clientId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No application with client id " + clientId));
        return new Detail(summary.objectId(), summary.clientId(), summary.displayName(), summary.created(),
                grantedRoles(clientId));
    }

    /** Takes an API away again. The assignment lives on the API's principal, so that is where it is removed. */
    public void revokeApiAccess(final String consumerClientId, final String roleValue) {
        String apiPrincipalId = servicePrincipalObjectId(config.apiClientId(),
                "No service principal for the API " + config.apiClientId());
        UUID roleId = appRoleId(apiPrincipalId, roleValue);
        String assignmentId = assignmentIdFor(consumerClientId, roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Application " + consumerClientId + " does not hold " + roleValue));
        try {
            graph.servicePrincipals().byServicePrincipalId(apiPrincipalId)
                    .appRoleAssignedTo().byAppRoleAssignmentId(assignmentId).delete();
        } catch (final ApiException e) {
            throw assignmentRefused(e);
        }
        log.info("Revoked {} on {} from application {}", roleValue, config.apiClientId(), consumerClientId);
    }

    private List<String> grantedRoles(final String clientId) {
        String principalId = servicePrincipalObjectId(clientId, "No service principal for " + clientId);
        Map<UUID, String> namesById = rolesOfferedByTheApi();
        return assignmentsFor(principalId).stream()
                .map(assignment -> namesById.get(assignment.getAppRoleId()))
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private Optional<String> assignmentIdFor(final String consumerClientId, final UUID roleId) {
        String principalId = servicePrincipalObjectId(consumerClientId,
                "No service principal for " + consumerClientId);
        return assignmentsFor(principalId).stream()
                .filter(assignment -> roleId.equals(assignment.getAppRoleId()))
                .map(AppRoleAssignment::getId)
                .findFirst();
    }

    private List<AppRoleAssignment> assignmentsFor(final String principalId) {
        AppRoleAssignmentCollectionResponse response = graph.servicePrincipals()
                .byServicePrincipalId(principalId).appRoleAssignments().get();
        return response == null || response.getValue() == null ? List.of() : response.getValue();
    }

    private Map<UUID, String> rolesOfferedByTheApi() {
        String apiPrincipalId = servicePrincipalObjectId(config.apiClientId(),
                "No service principal for the API " + config.apiClientId());
        return graph.servicePrincipals().byServicePrincipalId(apiPrincipalId).get().getAppRoles().stream()
                .collect(Collectors.toMap(AppRole::getId, AppRole::getValue));
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

    public void delete(final String clientId) {
        deleteByObjectId(get(clientId).objectId());
    }

    /**
     * Used by the rollback, which already holds the object id. It must not look it up: the application
     * was created moments ago and may not have reached the replica a lookup would read.
     */
    private void deleteByObjectId(final String objectId) {
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
