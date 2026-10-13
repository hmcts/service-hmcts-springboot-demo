package uk.gov.hmcts.amp.entraapp.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.amp.entraapp.config.EntraProperties;
import reactor.core.publisher.Mono;

import java.util.List;
import java.time.OffsetDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

/** The real Graph SDK against a mock Entra, so the calls it makes are visible. */
class EntraAppRegistrationServiceTest {

    private static final String OBJECT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CLIENT_ID = "22222222-2222-2222-2222-222222222222";
    private static final String KEY_ID = "33333333-3333-3333-3333-333333333333";
    private static final int ATTEMPTS = 4;
    private static final String API_CLIENT_ID = "99999999-9999-9999-9999-999999999999";
    private static final String API_SP_ID = "44444444-4444-4444-4444-444444444444";
    private static final String CONSUMER_SP_ID = "55555555-5555-5555-5555-555555555555";
    private static final String ROLE_ID = "66666666-6666-6666-6666-666666666666";

    private WireMockServer mockEntra;
    private EntraAppRegistrationService client;

    @BeforeEach
    void startMockEntra() {
        mockEntra = new WireMockServer(wireMockConfig().dynamicPort());
        mockEntra.start();

        // A token the mock never checks, so the test needs no credential and reaches no network.
        TokenCredential noRealCredential = request ->
                Mono.just(new AccessToken("a-graph-token", OffsetDateTime.MAX));
        GraphServiceClient sdk = new GraphServiceClient(noRealCredential, "https://graph.microsoft.com/.default");
        sdk.getRequestAdapter().setBaseUrl(mockEntra.baseUrl());

        client = new EntraAppRegistrationService(sdk, new EntraProperties(
                "a-tenant", "an-onboarding-app", "a-secret", mockEntra.baseUrl(), mockEntra.baseUrl(), API_CLIENT_ID,
                ATTEMPTS, 1));
    }

    @AfterEach
    void stopMockEntra() {
        mockEntra.stop();
    }

    @Test
    void registering_an_application_should_make_the_three_graph_calls_and_return_the_secret() {
        stubCreateApplication();
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals")).willReturn(json("{\"id\":\"sp-1\"}")));
        stubAddPassword();

        EntraAppRegistrationService.Registration registration = client.register("Team Alpha");

        // Both ids come back: the caller needs clientId to use the application and objectId to remove it.
        assertThat(registration.objectId()).isEqualTo(OBJECT_ID);
        assertThat(registration.clientId()).isEqualTo(CLIENT_ID);
        assertThat(registration.clientSecret()).isEqualTo("the-secret-shown-once");
        assertThat(registration.keyId()).isEqualTo(KEY_ID);

        mockEntra.verify(postRequestedFor(urlPathEqualTo("/applications"))
                .withRequestBody(equalToJson("{\"displayName\":\"Team Alpha\"}", true, true)));
        // The service principal is created from the client id, not the object id.
        mockEntra.verify(postRequestedFor(urlPathEqualTo("/servicePrincipals"))
                .withRequestBody(equalToJson("{\"appId\":\"" + CLIENT_ID + "\"}", true, true)));
        // addPassword addresses the application by object id, which only the create response carries.
        mockEntra.verify(postRequestedFor(urlPathEqualTo("/applications/" + OBJECT_ID + "/addPassword")));
    }

    @Test
    void a_service_principal_that_404s_on_a_stale_replica_should_be_retried_until_it_appears() {
        stubCreateApplication();
        stubAddPassword();
        // Graph's read replicas lag the write that created the application: the first attempt misses it.
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals")).inScenario("replication")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(404))
                .willSetStateTo("replicated"));
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals")).inScenario("replication")
                .whenScenarioStateIs("replicated")
                .willReturn(json("{\"id\":\"sp-1\"}")));

        EntraAppRegistrationService.Registration registration = client.register("Team Alpha");

        assertThat(registration.clientId()).isEqualTo(CLIENT_ID);
        mockEntra.verify(2, postRequestedFor(urlPathEqualTo("/servicePrincipals")));
    }

    @Test
    void a_service_principal_that_never_appears_should_give_up_rather_than_retry_forever() {
        stubCreateApplication();
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.register("Team Alpha"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Could not register the application");

        mockEntra.verify(ATTEMPTS, postRequestedFor(urlPathEqualTo("/servicePrincipals")));
    }

    @Test
    void registering_with_roles_should_grant_them_in_the_same_call() {
        SubscriptionStubs.registration(mockEntra, OBJECT_ID, CLIENT_ID, KEY_ID);
        stubServicePrincipalLookups();
        stubApiRoles();
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .willReturn(json("{\"id\":\"assignment-1\"}")));

        EntraAppRegistrationService.Registration registration =
                client.register("Team Alpha", List.of("slc.read"));

        assertThat(registration.roles()).containsExactly("slc.read");
        mockEntra.verify(postRequestedFor(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo")));
    }

    @Test
    void a_service_principal_not_yet_replicated_should_be_waited_for_rather_than_failing() {
        SubscriptionStubs.registration(mockEntra, OBJECT_ID, CLIENT_ID, KEY_ID);
        stubApiRoles();
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals"))
                .withQueryParam("$filter", equalTo("appId eq '" + API_CLIENT_ID + "'"))
                .willReturn(json("{\"value\":[{\"id\":\"" + API_SP_ID + "\"}]}")));
        // The consumer's own principal was made seconds ago: the first read misses it.
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals"))
                .withQueryParam("$filter", equalTo("appId eq '" + CLIENT_ID + "'"))
                .inScenario("replication").whenScenarioStateIs(STARTED)
                .willReturn(json("{\"value\":[]}"))
                .willSetStateTo("replicated"));
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals"))
                .withQueryParam("$filter", equalTo("appId eq '" + CLIENT_ID + "'"))
                .inScenario("replication").whenScenarioStateIs("replicated")
                .willReturn(json("{\"value\":[{\"id\":\"" + CONSUMER_SP_ID + "\"}]}")));
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .willReturn(json("{\"id\":\"assignment-1\"}")));

        EntraAppRegistrationService.Registration registration =
                client.register("Team Alpha", List.of("slc.read"));

        assertThat(registration.roles()).containsExactly("slc.read");
    }

    @Test
    void a_role_that_cannot_be_granted_should_take_the_new_application_away_again() {
        SubscriptionStubs.registration(mockEntra, OBJECT_ID, CLIENT_ID, KEY_ID);
        stubServicePrincipalLookups();
        stubApiRoles();
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .willReturn(aResponse().withStatus(403)));
        mockEntra.stubFor(delete(urlPathMatching("/applications/.*")).willReturn(aResponse().withStatus(204)));

        assertThatThrownBy(() -> client.register("Team Alpha", List.of("slc.read")))
                .isInstanceOf(ResponseStatusException.class);

        // A registration whose secret was shown but whose access failed is worse than none.
        mockEntra.verify(deleteRequestedFor(urlPathEqualTo("/applications/" + OBJECT_ID)));
    }

    @Test
    void a_registration_that_fails_after_the_application_exists_should_not_leave_it_behind() {
        stubCreateApplication();
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals")).willReturn(aResponse().withStatus(404)));
        mockEntra.stubFor(delete(urlPathMatching("/applications/.*")).willReturn(aResponse().withStatus(204)));

        assertThatThrownBy(() -> client.register("Team Alpha")).isInstanceOf(ResponseStatusException.class);

        // The application was created before the failure, so it has to be taken away again.
        mockEntra.verify(deleteRequestedFor(urlPathEqualTo("/applications/" + OBJECT_ID)));
    }

    @Test
    void a_cleanup_that_also_fails_should_still_report_the_registration_failure() {
        stubCreateApplication();
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals")).willReturn(aResponse().withStatus(404)));
        mockEntra.stubFor(delete(urlPathMatching("/applications/.*")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.register("Team Alpha"))
                .isInstanceOf(ResponseStatusException.class)
                // Not "could not delete": the caller needs to know registration failed.
                .hasMessageContaining("Could not register the application")
                .satisfies(failure -> assertThat(failure.getSuppressed()).isNotEmpty());
    }

    @Test
    void listing_applications_should_return_what_is_needed_to_recognise_and_remove_them() {
        mockEntra.stubFor(get(urlPathEqualTo("/applications")).willReturn(json("""
                {"value":[
                  {"id":"obj-2","appId":"app-2","displayName":"newer","createdDateTime":"2026-06-07T08:09:10Z"},
                  {"id":"obj-1","appId":"app-1","displayName":"older","createdDateTime":"2026-01-02T03:04:05Z"}
                ]}""")));

        List<EntraAppRegistrationService.Summary> found = client.list();

        assertThat(found).extracting(EntraAppRegistrationService.Summary::displayName)
                .containsExactly("older", "newer");
        assertThat(found.getFirst().objectId()).isEqualTo("obj-1");
        assertThat(found.getFirst().created()).isNotNull();
        mockEntra.verify(getRequestedFor(urlPathEqualTo("/applications")).withoutQueryParam("$orderby"));
    }

    @Test
    void listing_applications_should_follow_graphs_paging_rather_than_stop_at_the_first_page() {
        mockEntra.stubFor(get(urlPathEqualTo("/applications")).willReturn(json("""
                {"value":[{"id":"obj-1","appId":"app-1","displayName":"page one"}],
                 "@odata.nextLink":"%s/applications?$skiptoken=abc"}""".formatted(mockEntra.baseUrl()))));
        mockEntra.stubFor(get(urlPathEqualTo("/applications")).withQueryParam("$skiptoken", equalTo("abc"))
                .willReturn(json("""
                {"value":[{"id":"obj-2","appId":"app-2","displayName":"page two"}]}""")));

        List<EntraAppRegistrationService.Summary> found = client.list();

        assertThat(found).extracting(EntraAppRegistrationService.Summary::displayName)
                .containsExactly("page one", "page two");
    }

    @Test
    void granting_api_access_should_assign_the_role_on_the_apis_service_principal() {
        stubServicePrincipalLookups();
        stubApiRoles();
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .willReturn(json("{\"id\":\"assignment-1\"}")));

        client.grantApiAccess(CLIENT_ID, "slc.read");

        // Assigned on the API's principal, naming the consumer as principalId - not the other way round.
        mockEntra.verify(postRequestedFor(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .withRequestBody(equalToJson("""
                        {"principalId":"%s","resourceId":"%s","appRoleId":"%s"}"""
                        .formatted(CONSUMER_SP_ID, API_SP_ID, ROLE_ID), true, true)));
    }

    @Test
    void a_role_the_api_does_not_offer_should_say_what_it_does_offer() {
        stubServicePrincipalLookups();
        stubApiRoles();

        assertThatThrownBy(() -> client.grantApiAccess(CLIENT_ID, "nonsense.read"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("slc.read");
    }

    @Test
    void an_assignment_whose_reference_is_not_found_yet_should_be_retried() {
        stubServicePrincipalLookups();
        stubApiRoles();
        // Graph's other way of saying the principal has not replicated: 404 "does not exist".
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .inScenario("missing").whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(404)).willSetStateTo("present"));
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .inScenario("missing").whenScenarioStateIs("present")
                .willReturn(json("{\"id\":\"assignment-1\"}")));

        client.grantApiAccess(CLIENT_ID, "slc.read");

        mockEntra.verify(2, postRequestedFor(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo")));
    }

    @Test
    void an_assignment_whose_reference_is_not_valid_yet_should_be_retried() {
        stubServicePrincipalLookups();
        stubApiRoles();
        // Graph says 400 "Not a valid reference update" while the principal is still replicating.
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .inScenario("reference").whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(400)).willSetStateTo("valid"));
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .inScenario("reference").whenScenarioStateIs("valid")
                .willReturn(json("{\"id\":\"assignment-1\"}")));

        client.grantApiAccess(CLIENT_ID, "slc.read");

        mockEntra.verify(2, postRequestedFor(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo")));
    }

    @Test
    void a_refused_assignment_should_name_the_permission_that_is_missing() {
        stubServicePrincipalLookups();
        stubApiRoles();
        mockEntra.stubFor(post(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo"))
                .willReturn(aResponse().withStatus(403)));

        assertThatThrownBy(() -> client.grantApiAccess(CLIENT_ID, "slc.read"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AppRoleAssignment.ReadWrite.All")
                // Not 503: a missing consent grant is not temporary, and 503 invites endless retrying.
                .asInstanceOf(throwable(ResponseStatusException.class))
                .extracting(ResponseStatusException::getStatusCode)
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // Tried once, not retried: a permission refusal will not improve by asking again.
        mockEntra.verify(1, postRequestedFor(urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo")));
    }

    private void stubServicePrincipalLookups() {
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals"))
                .withQueryParam("$filter", equalTo("appId eq '" + CLIENT_ID + "'"))
                .willReturn(json("{\"value\":[{\"id\":\"" + CONSUMER_SP_ID + "\"}]}")));
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals"))
                .withQueryParam("$filter", equalTo("appId eq '" + API_CLIENT_ID + "'"))
                .willReturn(json("{\"value\":[{\"id\":\"" + API_SP_ID + "\"}]}")));
    }

    @Test
    void deleting_an_application_should_find_its_object_id_from_the_client_id() {
        stubApplicationList();
        stubServicePrincipalLookups();
        stubApiRoles();
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals/" + CONSUMER_SP_ID + "/appRoleAssignments"))
                .willReturn(json("{\"value\":[]}")));
        mockEntra.stubFor(delete(urlPathMatching("/applications/.*")).willReturn(aResponse().withStatus(204)));

        client.delete(CLIENT_ID);

        // The caller names the client id; Graph needs the object id, which is looked up rather than asked for.
        mockEntra.verify(deleteRequestedFor(urlPathEqualTo("/applications/" + OBJECT_ID)));
    }

    @Test
    void reading_one_application_should_name_the_roles_it_holds() {
        stubApplicationList();
        stubServicePrincipalLookups();
        stubApiRoles();
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals/" + CONSUMER_SP_ID + "/appRoleAssignments"))
                .willReturn(json("""
                        {"value":[{"id":"assign-1","appRoleId":"%s"}]}""".formatted(ROLE_ID))));

        EntraAppRegistrationService.Detail detail = client.get(CLIENT_ID);

        // Graph answers with the role's id; the caller wants its name.
        assertThat(detail.roles()).containsExactly("slc.read");
        assertThat(detail.clientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    void revoking_a_role_should_delete_the_assignment_on_the_apis_principal() {
        stubServicePrincipalLookups();
        stubApiRoles();
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals/" + CONSUMER_SP_ID + "/appRoleAssignments"))
                .willReturn(json("""
                        {"value":[{"id":"assign-1","appRoleId":"%s"}]}""".formatted(ROLE_ID))));
        mockEntra.stubFor(delete(urlPathMatching("/servicePrincipals/.*/appRoleAssignedTo/.*"))
                .willReturn(aResponse().withStatus(204)));

        client.revokeApiAccess(CLIENT_ID, "slc.read");

        mockEntra.verify(deleteRequestedFor(
                urlPathEqualTo("/servicePrincipals/" + API_SP_ID + "/appRoleAssignedTo/assign-1")));
    }

    @Test
    void revoking_a_role_the_application_does_not_hold_should_be_not_found() {
        stubServicePrincipalLookups();
        stubApiRoles();
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals/" + CONSUMER_SP_ID + "/appRoleAssignments"))
                .willReturn(json("{\"value\":[]}")));

        assertThatThrownBy(() -> client.revokeApiAccess(CLIENT_ID, "slc.read"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not hold slc.read");
    }

    private static final class SubscriptionStubs {
        static void registration(final com.github.tomakehurst.wiremock.WireMockServer mock,
                                 final String objectId, final String clientId, final String keyId) {
            mock.stubFor(post(urlPathEqualTo("/applications")).willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":\"" + objectId + "\",\"appId\":\"" + clientId + "\"}")));
            mock.stubFor(post(urlPathEqualTo("/servicePrincipals")).willReturn(aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json").withBody("{\"id\":\"sp-1\"}")));
            mock.stubFor(post(urlPathEqualTo("/applications/" + objectId + "/addPassword"))
                    .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                            .withBody("{\"keyId\":\"" + keyId + "\",\"secretText\":\"the-secret-shown-once\"}")));
        }
    }

    private void stubApplicationList() {
        mockEntra.stubFor(get(urlPathEqualTo("/applications")).willReturn(json("""
                {"value":[{"id":"%s","appId":"%s","displayName":"Team Alpha"}]}"""
                .formatted(OBJECT_ID, CLIENT_ID))));
    }

    private void stubApiRoles() {
        mockEntra.stubFor(get(urlPathEqualTo("/servicePrincipals/" + API_SP_ID)).willReturn(json("""
                {"id":"%s","appRoles":[{"id":"%s","value":"slc.read"}]}""".formatted(API_SP_ID, ROLE_ID))));
    }

    private void stubCreateApplication() {
        mockEntra.stubFor(post(urlPathEqualTo("/applications"))
                .willReturn(json("{\"id\":\"" + OBJECT_ID + "\",\"appId\":\"" + CLIENT_ID + "\"}")));
    }

    private void stubAddPassword() {
        mockEntra.stubFor(post(urlPathEqualTo("/applications/" + OBJECT_ID + "/addPassword"))
                .willReturn(json("{\"keyId\":\"" + KEY_ID + "\",\"secretText\":\"the-secret-shown-once\"}")));
    }

    private com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(final String body) {
        return aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body);
    }
}
