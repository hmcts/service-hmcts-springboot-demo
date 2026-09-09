package uk.gov.hmcts.amp.entra.emulator.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.amp.entra.emulator.model.AppSecret;
import uk.gov.hmcts.amp.entra.emulator.model.ApplicationRegistration;
import uk.gov.hmcts.amp.entra.emulator.model.DirectoryUser;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EntraAdminClientTest extends EntraWireMockTestBase {

    private static final String APP_ID = "e16ecaee-94aa-4d31-893b-4f33f0834516";

    @Autowired
    EntraAdminClient adminClient;

    @BeforeEach
    void resetStubs() {
        WIRE_MOCK.resetAll();
    }

    @Test
    void creating_a_confidential_application_should_return_the_new_registration() {
        stubFor(WireMock.post(urlEqualTo("/admin/api/apps"))
                .withRequestBody(equalToJson("{\"displayName\":\"AMP-1106 Job\",\"isConfidential\":true}"))
                .willReturn(jsonResponse(HTTP_CREATED, """
                        {
                          "id": "%s",
                          "displayName": "AMP-1106 Job",
                          "isConfidential": true,
                          "appIdUri": null,
                          "appRoles": [],
                          "secrets": []
                        }
                        """.formatted(APP_ID))));

        final ApplicationRegistration application = adminClient.createApplication("AMP-1106 Job", true);

        assertThat(application.id()).isEqualTo(APP_ID);
        assertThat(application.isConfidential()).isTrue();
    }

    @Test
    void adding_a_secret_should_return_the_secret_text_only_available_at_creation() {
        stubFor(WireMock.post(urlEqualTo("/admin/api/apps/" + APP_ID + "/secrets"))
                .willReturn(jsonResponse(HTTP_CREATED, """
                        {
                          "id": "2e978b34-2ef9-4673-a2f3-037b4cb23568",
                          "displayName": "job-secret",
                          "hint": "VXd…qg",
                          "secretText": "VXdxLJpoX2HLtnL1yb5K5GsqC9uxFrbV48ZgsvvSaqg"
                        }
                        """)));

        final AppSecret secret = adminClient.addSecret(APP_ID, "job-secret");

        assertThat(secret.secretText()).isEqualTo("VXdxLJpoX2HLtnL1yb5K5GsqC9uxFrbV48ZgsvvSaqg");
    }

    @Test
    void listing_applications_should_unwrap_the_value_array() {
        stubFor(WireMock.get(urlEqualTo("/admin/api/apps"))
                .willReturn(jsonResponse(HTTP_OK, """
                        {
                          "value": [
                            {"id": "cccccccc-0000-0000-0000-000000000002", "displayName": "Sample Daemon", "isConfidential": true}
                          ]
                        }
                        """)));

        assertThat(adminClient.listApplications())
                .extracting(ApplicationRegistration::displayName)
                .containsExactly("Sample Daemon");
    }

    @Test
    void creating_a_user_should_post_the_principal_name_and_return_the_created_user() {
        stubFor(WireMock.post(urlEqualTo("/admin/api/users"))
                .willReturn(jsonResponse(HTTP_CREATED, """
                        {
                          "id": "8d2f7252-6e2e-4e13-9789-a240f1b26daa",
                          "userPrincipalName": "colin.test@entralocal.dev",
                          "displayName": "Colin Test",
                          "accountEnabled": true
                        }
                        """)));

        final DirectoryUser user = adminClient.createUser("Colin Test", "colin.test@entralocal.dev", "Password1!");

        assertThat(user.userPrincipalName()).isEqualTo("colin.test@entralocal.dev");
        WireMock.verify(postRequestedFor(urlEqualTo("/admin/api/users"))
                .withRequestBody(equalToJson("""
                        {"displayName":"Colin Test","userPrincipalName":"colin.test@entralocal.dev","password":"Password1!"}
                        """)));
    }

    @Test
    void reading_the_emulator_health_should_report_the_configured_tenant_id() {
        stubFor(WireMock.get(urlEqualTo("/admin/api/health"))
                .willReturn(jsonResponse(HTTP_OK, """
                        {"status":"ok","tenantId":"11111111-1111-1111-1111-111111111111"}
                        """)));

        assertThat(adminClient.tenantId()).isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder jsonResponse(final int status, final String body) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body);
    }
}
