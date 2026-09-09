package uk.gov.hmcts.amp.entra.emulator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.amp.entra.emulator.client.EntraAdminClient;
import uk.gov.hmcts.amp.entra.emulator.config.EntraProperties;
import uk.gov.hmcts.amp.entra.emulator.model.AppRole;
import uk.gov.hmcts.amp.entra.emulator.model.AppSecret;
import uk.gov.hmcts.amp.entra.emulator.model.ApplicationRegistration;
import uk.gov.hmcts.amp.entra.emulator.model.DaemonApplication;
import uk.gov.hmcts.amp.entra.emulator.model.DirectoryUser;
import uk.gov.hmcts.amp.entra.emulator.model.TenantInfo;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntraDirectoryService {

    private final EntraAdminClient adminClient;
    private final EntraProperties properties;

    /**
     * The emulator hosts a single tenant fixed by its TENANT_ID environment variable. Entra has no
     * tenant-creation API either — tenants are created in the portal — so this reports rather than creates.
     */
    public TenantInfo tenant() {
        return new TenantInfo(adminClient.tenantId(),
                properties.issuer(),
                properties.tokenEndpoint(),
                properties.discoveryEndpoint());
    }

    public ApplicationRegistration createApplication(final String displayName, final boolean confidential) {
        return adminClient.createApplication(displayName, confidential);
    }

    public AppSecret addSecret(final String applicationId, final String secretName) {
        return adminClient.addSecret(applicationId, secretName);
    }

    public AppRole addApplicationRole(final String applicationId, final String role, final String description) {
        return adminClient.addApplicationRole(applicationId, AppRole.applicationRole(role, description));
    }

    public DirectoryUser createUser(final String displayName, final String userPrincipalName, final String password) {
        return adminClient.createUser(displayName, userPrincipalName, password);
    }

    public List<ApplicationRegistration> applications() {
        return adminClient.listApplications();
    }

    public List<DirectoryUser> users() {
        return adminClient.listUsers();
    }

    public DaemonApplication provisionDaemonApplication(final String displayName,
                                                        final String appIdUri,
                                                        final List<String> appRoles) {
        final ApplicationRegistration application = adminClient.createApplication(displayName, true);
        adminClient.setApplicationIdUri(application.id(), appIdUri);
        appRoles.forEach(role -> adminClient.addApplicationRole(application.id(), AppRole.applicationRole(role, role)));
        final AppSecret secret = adminClient.addSecret(application.id(), displayName + " secret");
        log.info("Provisioned daemon application {} with {} role(s)", application.id(), appRoles.size());
        return new DaemonApplication(application.id(), appIdUri, secret.secretText(), appRoles);
    }
}
