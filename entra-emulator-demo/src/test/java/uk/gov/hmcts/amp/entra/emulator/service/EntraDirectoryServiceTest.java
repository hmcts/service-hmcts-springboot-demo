package uk.gov.hmcts.amp.entra.emulator.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.amp.entra.emulator.client.EntraAdminClient;
import uk.gov.hmcts.amp.entra.emulator.config.EntraProperties;
import uk.gov.hmcts.amp.entra.emulator.model.AppRole;
import uk.gov.hmcts.amp.entra.emulator.model.AppSecret;
import uk.gov.hmcts.amp.entra.emulator.model.ApplicationRegistration;
import uk.gov.hmcts.amp.entra.emulator.model.DaemonApplication;
import uk.gov.hmcts.amp.entra.emulator.model.TenantInfo;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntraDirectoryServiceTest {

    private static final String TENANT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String APP_ID = "e16ecaee-94aa-4d31-893b-4f33f0834516";

    private final EntraAdminClient adminClient = mock(EntraAdminClient.class);
    private final EntraProperties properties =
            new EntraProperties("https://localhost:8443", TENANT_ID, "https://localhost:8443/admin/api", true);
    private final EntraDirectoryService directoryService = new EntraDirectoryService(adminClient, properties);

    @Test
    void provisioning_a_daemon_application_should_return_credentials_ready_for_a_token_request() {
        when(adminClient.createApplication(anyString(), anyBoolean()))
                .thenReturn(new ApplicationRegistration(APP_ID, "AMP-1106 Job", true, null, List.of(), List.of()));
        when(adminClient.addSecret(anyString(), anyString()))
                .thenReturn(new AppSecret("secret-id", "AMP-1106 Job secret", "VXd…qg", "the-secret-text"));

        final DaemonApplication daemon =
                directoryService.provisionDaemonApplication("AMP-1106 Job", "api://amp-1106-job", List.of("app.read"));

        assertThat(daemon.clientId()).isEqualTo(APP_ID);
        assertThat(daemon.clientSecret()).isEqualTo("the-secret-text");
        assertThat(daemon.appIdUri()).isEqualTo("api://amp-1106-job");
        assertThat(daemon.appRoles()).containsExactly("app.read");
    }

    @Test
    void provisioning_a_daemon_application_should_register_each_requested_app_role() {
        when(adminClient.createApplication(anyString(), anyBoolean()))
                .thenReturn(new ApplicationRegistration(APP_ID, "AMP-1106 Job", true, null, List.of(), List.of()));
        when(adminClient.addSecret(anyString(), anyString()))
                .thenReturn(new AppSecret("secret-id", "secret", "hint", "the-secret-text"));

        directoryService.provisionDaemonApplication("AMP-1106 Job", "api://amp-1106-job", List.of("app.read", "app.write"));

        verify(adminClient).setApplicationIdUri(APP_ID, "api://amp-1106-job");
        verify(adminClient, times(2)).addApplicationRole(anyString(), any(AppRole.class));
    }

    @Test
    void asking_for_the_tenant_should_report_the_emulator_tenant_and_derived_endpoints() {
        when(adminClient.tenantId()).thenReturn(TENANT_ID);

        final TenantInfo tenant = directoryService.tenant();

        assertThat(tenant.tenantId()).isEqualTo(TENANT_ID);
        assertThat(tenant.issuer()).isEqualTo("https://localhost:8443/" + TENANT_ID + "/v2.0");
        assertThat(tenant.tokenEndpoint()).isEqualTo("https://localhost:8443/" + TENANT_ID + "/oauth2/v2.0/token");
        assertThat(tenant.discoveryEndpoint())
                .isEqualTo("https://localhost:8443/" + TENANT_ID + "/v2.0/.well-known/openid-configuration");
    }
}
