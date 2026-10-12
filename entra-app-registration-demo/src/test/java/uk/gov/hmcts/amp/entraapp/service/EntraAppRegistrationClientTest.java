package uk.gov.hmcts.amp.entraapp.service;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
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

/** The real Graph SDK against a mock Entra, so the calls it makes are visible. */
class EntraAppRegistrationClientTest {

    private static final String OBJECT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String CLIENT_ID = "22222222-2222-2222-2222-222222222222";
    private static final String KEY_ID = "33333333-3333-3333-3333-333333333333";

    private WireMockServer mockEntra;
    private EntraAppRegistrationClient client;

    @BeforeEach
    void startMockEntra() {
        mockEntra = new WireMockServer(wireMockConfig().dynamicPort());
        mockEntra.start();

        // A token the mock never checks, so the test needs no credential and reaches no network.
        TokenCredential noRealCredential = request ->
                Mono.just(new AccessToken("a-graph-token", OffsetDateTime.MAX));
        GraphServiceClient sdk = new GraphServiceClient(noRealCredential, "https://graph.microsoft.com/.default");
        sdk.getRequestAdapter().setBaseUrl(mockEntra.baseUrl());

        client = new EntraAppRegistrationClient(sdk);
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

        EntraAppRegistrationClient.Registration registration = client.register("Team Alpha");

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

        EntraAppRegistrationClient.Registration registration = client.register("Team Alpha");

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

        mockEntra.verify(4, postRequestedFor(urlPathEqualTo("/servicePrincipals")));
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

        List<EntraAppRegistrationClient.Summary> found = client.list();

        assertThat(found).extracting(EntraAppRegistrationClient.Summary::displayName)
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

        List<EntraAppRegistrationClient.Summary> found = client.list();

        assertThat(found).extracting(EntraAppRegistrationClient.Summary::displayName)
                .containsExactly("page one", "page two");
    }

    @Test
    void deleting_an_application_should_address_it_by_object_id() {
        mockEntra.stubFor(delete(urlPathMatching("/applications/.*")).willReturn(aResponse().withStatus(204)));

        client.delete(OBJECT_ID);

        mockEntra.verify(deleteRequestedFor(urlPathEqualTo("/applications/" + OBJECT_ID)));
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
