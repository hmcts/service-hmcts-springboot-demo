package uk.gov.hmcts.amp.apimkeys.service;

import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.apimanagement.ApiManagementManager;
import com.azure.resourcemanager.apimanagement.models.SubscriptionContract;
import com.azure.resourcemanager.apimanagement.models.SubscriptionCreateParameters;
import com.azure.resourcemanager.apimanagement.models.SubscriptionKeysContract;
import com.azure.resourcemanager.apimanagement.models.SubscriptionState;
import com.azure.resourcemanager.apimanagement.models.Subscriptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.amp.apimkeys.config.ApimProperties;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApimSubscriptionKeyServiceTest {

    private static final String RESOURCE_GROUP = "rg-demo";
    private static final String SERVICE_NAME = "apim-demo";

    private Subscriptions subscriptions;
    private ApimSubscriptionKeyService service;

    @BeforeEach
    void setUp() {
        subscriptions = mock(Subscriptions.class);
        ApiManagementManager apim = mock(ApiManagementManager.class);
        when(apim.subscriptions()).thenReturn(subscriptions);
        service = new ApimSubscriptionKeyService(apim,
            new ApimProperties("a-tenant", "an-azure-subscription", RESOURCE_GROUP, SERVICE_NAME));
    }

    @Test
    void creating_a_subscription_should_scope_it_to_the_product_and_return_its_key() {
        SubscriptionContract azureCreated = contract("team-alpha", "Team Alpha");
        when(subscriptions.createOrUpdate(eq(RESOURCE_GROUP), eq(SERVICE_NAME), eq("team-alpha"), any()))
            .thenReturn(azureCreated);
        givenSecretsFor("team-alpha");

        SubscriptionKey created = service.create("team-alpha", "hearing-results", "Team Alpha");

        ArgumentCaptor<SubscriptionCreateParameters> sent =
            ArgumentCaptor.forClass(SubscriptionCreateParameters.class);
        verify(subscriptions).createOrUpdate(eq(RESOURCE_GROUP), eq(SERVICE_NAME), eq("team-alpha"),
            sent.capture());
        assertThat(sent.getValue().scope()).isEqualTo("/products/hearing-results");
        assertThat(sent.getValue().displayName()).isEqualTo("Team Alpha");
        assertThat(created.primaryKey()).isEqualTo("primary-key-value");
        assertThat(created.secondaryKey()).isEqualTo("secondary-key-value");
    }

    @Test
    void listing_subscriptions_should_return_them_without_asking_azure_for_any_key() {
        Stream<SubscriptionContract> azureListed = Stream.of(contract("team-alpha", "Team Alpha"),
            contract("team-beta", "Team Beta"));
        PagedIterable<SubscriptionContract> page = mock(PagedIterable.class);
        when(page.stream()).thenReturn(azureListed);
        when(subscriptions.list(RESOURCE_GROUP, SERVICE_NAME)).thenReturn(page);

        List<SubscriptionKey> listed = service.list();

        assertThat(listed).extracting(SubscriptionKey::name).containsExactly("team-alpha", "team-beta");
        assertThat(listed).extracting(SubscriptionKey::primaryKey).containsOnlyNulls();
        verify(subscriptions, never()).listSecrets(any(), any(), any());
    }

    @Test
    void getting_a_subscription_should_ask_azure_separately_for_its_secrets() {
        SubscriptionContract azureFound = contract("team-alpha", "Team Alpha");
        when(subscriptions.get(RESOURCE_GROUP, SERVICE_NAME, "team-alpha")).thenReturn(azureFound);
        givenSecretsFor("team-alpha");

        SubscriptionKey found = service.get("team-alpha");

        assertThat(found.displayName()).isEqualTo("Team Alpha");
        assertThat(found.primaryKey()).isEqualTo("primary-key-value");
        verify(subscriptions).listSecrets(RESOURCE_GROUP, SERVICE_NAME, "team-alpha");
    }

    @Test
    void getting_a_subscription_that_is_not_there_should_be_not_found() {
        ManagementException notFound = azureStatus(404);
        when(subscriptions.get(RESOURCE_GROUP, SERVICE_NAME, "nobody")).thenThrow(notFound);

        assertThatThrownBy(() -> service.get("nobody"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("No subscription named nobody");
    }

    @Test
    void deleting_a_subscription_should_pass_the_if_match_azure_insists_on() {
        service.delete("team-alpha");

        verify(subscriptions).delete(RESOURCE_GROUP, SERVICE_NAME, "team-alpha", "*");
    }

    @Test
    void deleting_a_subscription_that_is_not_there_should_be_not_found() {
        doThrowOnDelete(azureStatus(404));

        assertThatThrownBy(() -> service.delete("nobody")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void an_azure_failure_that_is_not_a_missing_subscription_should_not_be_disguised_as_one() {
        doThrowOnDelete(azureStatus(403));

        assertThatThrownBy(() -> service.delete("team-alpha")).isInstanceOf(ManagementException.class);
    }

    private void doThrowOnDelete(final ManagementException failure) {
        org.mockito.Mockito.doThrow(failure).when(subscriptions)
            .delete(any(), any(), any(), any());
    }

    private void givenSecretsFor(final String name) {
        SubscriptionKeysContract secrets = mock(SubscriptionKeysContract.class);
        when(secrets.primaryKey()).thenReturn("primary-key-value");
        when(secrets.secondaryKey()).thenReturn("secondary-key-value");
        when(subscriptions.listSecrets(RESOURCE_GROUP, SERVICE_NAME, name)).thenReturn(secrets);
    }

    private SubscriptionContract contract(final String name, final String displayName) {
        SubscriptionContract contract = mock(SubscriptionContract.class);
        when(contract.name()).thenReturn(name);
        when(contract.displayName()).thenReturn(displayName);
        when(contract.scope()).thenReturn("/products/hearing-results");
        when(contract.state()).thenReturn(SubscriptionState.ACTIVE);
        return contract;
    }

    private ManagementException azureStatus(final int statusCode) {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getStatusCode()).thenReturn(statusCode);
        return new ManagementException("Azure said " + statusCode, response);
    }
}
