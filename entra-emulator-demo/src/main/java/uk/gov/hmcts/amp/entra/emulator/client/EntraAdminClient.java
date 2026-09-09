package uk.gov.hmcts.amp.entra.emulator.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.amp.entra.emulator.config.EntraProperties;
import uk.gov.hmcts.amp.entra.emulator.model.AppRole;
import uk.gov.hmcts.amp.entra.emulator.model.AppSecret;
import uk.gov.hmcts.amp.entra.emulator.model.ApplicationRegistration;
import uk.gov.hmcts.amp.entra.emulator.model.DirectoryListing;
import uk.gov.hmcts.amp.entra.emulator.model.DirectoryUser;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * The emulator's directory management API. Microsoft Graph is the real-Entra equivalent, but its
 * request and response shapes differ, so this client is deliberately emulator-only.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntraAdminClient {

    private static final ParameterizedTypeReference<DirectoryListing<ApplicationRegistration>> APPLICATION_LIST =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<DirectoryListing<DirectoryUser>> USER_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient entraRestClient;
    private final EntraProperties properties;

    public ApplicationRegistration createApplication(final String displayName, final boolean confidential) {
        log.info("Creating application registration {}", displayName);
        return post("/apps", Map.of("displayName", displayName, "isConfidential", confidential), ApplicationRegistration.class);
    }

    public ApplicationRegistration setApplicationIdUri(final String applicationId, final String appIdUri) {
        return entraRestClient.patch()
                .uri(admin("/apps/" + applicationId))
                .contentType(APPLICATION_JSON)
                .body(Map.of("appIdUri", appIdUri))
                .retrieve()
                .body(ApplicationRegistration.class);
    }

    public AppRole addApplicationRole(final String applicationId, final AppRole role) {
        log.info("Adding app role {} to application {}", role.value(), applicationId);
        return post("/apps/" + applicationId + "/roles", role, AppRole.class);
    }

    public AppSecret addSecret(final String applicationId, final String displayName) {
        log.info("Adding secret {} to application {}", displayName, applicationId);
        return post("/apps/" + applicationId + "/secrets", Map.of("displayName", displayName), AppSecret.class);
    }

    public DirectoryUser createUser(final String displayName, final String userPrincipalName, final String password) {
        log.info("Creating user {}", userPrincipalName);
        return post("/users", Map.of(
                "displayName", displayName,
                "userPrincipalName", userPrincipalName,
                "password", password), DirectoryUser.class);
    }

    public List<ApplicationRegistration> listApplications() {
        return entraRestClient.get().uri(admin("/apps")).retrieve().body(APPLICATION_LIST).value();
    }

    public List<DirectoryUser> listUsers() {
        return entraRestClient.get().uri(admin("/users")).retrieve().body(USER_LIST).value();
    }

    public String tenantId() {
        final Map<?, ?> health = entraRestClient.get().uri(admin("/health")).retrieve().body(Map.class);
        return String.valueOf(health.get("tenantId"));
    }

    private <T> T post(final String path, final Object body, final Class<T> responseType) {
        return entraRestClient.post()
                .uri(admin(path))
                .contentType(APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    private String admin(final String path) {
        return properties.adminBaseUrl() + path;
    }
}
