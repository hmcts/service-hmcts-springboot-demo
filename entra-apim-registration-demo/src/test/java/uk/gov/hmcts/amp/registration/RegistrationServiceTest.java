package uk.gov.hmcts.amp.registration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.amp.registration.apim.ApimClient;
import uk.gov.hmcts.amp.registration.config.RegistrationProperties;
import uk.gov.hmcts.amp.registration.entra.EntraGraphClient;
import uk.gov.hmcts.amp.registration.service.RegisteredApplication;
import uk.gov.hmcts.amp.registration.service.RegistrationService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private EntraGraphClient entra;

    @Mock
    private ApimClient apim;

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        RegistrationProperties properties = new RegistrationProperties(null,
            new RegistrationProperties.Apim(null, null, null, null, null, null, null,
                Map.of("hearing-results", "product-1", "court-listings", "product-2")));
        service = new RegistrationService(entra, apim, properties);
    }

    private RegistrationService.Created created(final String owner) {
        when(entra.register("My App")).thenReturn(new EntraGraphClient.Registration("client-1", "s3cret", "key-1"));
        return service.create(owner, "My App");
    }

    private void assertRejected(final Runnable action, final HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }

    @Test
    void creating_should_return_the_client_id_and_the_secret_once() {
        RegistrationService.Created created = created("olive");

        assertThat(created.clientSecret()).isEqualTo("s3cret");
        assertThat(created.application().clientId()).isEqualTo("client-1");
        assertThat(created.application().owner()).isEqualTo("olive");
        assertThat(service.list("olive")).containsExactly(created.application());
    }

    @Test
    void a_name_is_required_and_is_limited_in_length_and_entra_is_not_asked_when_it_is_refused() {
        assertRejected(() -> service.create("olive", " "), HttpStatus.BAD_REQUEST);
        assertRejected(() -> service.create("olive", null), HttpStatus.BAD_REQUEST);
        assertRejected(() -> service.create("olive", "x".repeat(RegistrationService.MAX_NAME + 1)),
            HttpStatus.BAD_REQUEST);

        verify(entra, never()).register(any());
    }

    @Test
    void a_failure_at_entra_should_leave_nothing_behind() {
        when(entra.register("My App")).thenThrow(new IllegalStateException("graph is down"));

        assertThatThrownBy(() -> service.create("olive", "My App")).isInstanceOf(IllegalStateException.class);

        assertThat(service.list("olive")).isEmpty();
    }

    @Test
    void connecting_an_api_should_get_a_subscription_key_for_its_product() {
        RegisteredApplication application = created("olive").application();
        when(apim.subscribe("My App", "product-1"))
            .thenReturn(new ApimClient.Subscription("product-1", "my-app-1", "the-key"));

        service.connect("olive", application.id().toString(), "hearing-results");

        assertThat(application.connections()).hasSize(1);
        assertThat(application.connections().get(0).subscription().subscriptionKey()).isEqualTo("the-key");
    }

    @Test
    void an_api_that_is_not_on_offer_is_refused_before_apim_is_asked() {
        RegisteredApplication application = created("olive").application();

        assertRejected(() -> service.connect("olive", application.id().toString(), "nope"), HttpStatus.BAD_REQUEST);
        assertRejected(() -> service.connect("olive", application.id().toString(), null), HttpStatus.BAD_REQUEST);

        verify(apim, never()).subscribe(any(), any());
    }

    @Test
    void an_api_connected_twice_is_refused_and_gets_one_key() {
        RegisteredApplication application = created("olive").application();
        when(apim.subscribe("My App", "product-1"))
            .thenReturn(new ApimClient.Subscription("product-1", "my-app-1", "the-key"));
        service.connect("olive", application.id().toString(), "hearing-results");

        assertRejected(() -> service.connect("olive", application.id().toString(), "hearing-results"),
            HttpStatus.CONFLICT);

        verify(apim).subscribe(any(), any());
    }

    @Test
    void someone_elses_application_and_one_that_does_not_exist_get_the_same_answer() {
        RegisteredApplication application = created("olive").application();

        assertRejected(() -> service.connect("someone-else", application.id().toString(), "hearing-results"),
            HttpStatus.NOT_FOUND);
        assertRejected(() -> service.delete("someone-else", application.id().toString()), HttpStatus.NOT_FOUND);
        assertRejected(() -> service.delete("olive", "not-a-uuid"), HttpStatus.NOT_FOUND);
        assertRejected(() -> service.delete("olive", "3f8a1c94-6b2e-4d51-9a77-0e1c5b8d4f23"), HttpStatus.NOT_FOUND);
        assertThat(service.list("someone-else")).isEmpty();
    }

    @Test
    void disconnecting_should_delete_the_subscription_first_and_forget_it() {
        RegisteredApplication application = created("olive").application();
        when(apim.subscribe("My App", "product-1"))
            .thenReturn(new ApimClient.Subscription("product-1", "my-app-1", "the-key"));
        service.connect("olive", application.id().toString(), "hearing-results");

        service.disconnect("olive", application.id().toString(), "hearing-results");

        verify(apim).unsubscribe("my-app-1");
        assertThat(application.connections()).isEmpty();
    }

    @Test
    void a_subscription_apim_will_not_delete_should_stay_connected() {
        RegisteredApplication application = created("olive").application();
        when(apim.subscribe("My App", "product-1"))
            .thenReturn(new ApimClient.Subscription("product-1", "my-app-1", "the-key"));
        service.connect("olive", application.id().toString(), "hearing-results");
        doThrow(new IllegalStateException("apim is down")).when(apim).unsubscribe("my-app-1");

        assertThatThrownBy(() -> service.disconnect("olive", application.id().toString(), "hearing-results"))
            .isInstanceOf(IllegalStateException.class);

        assertThat(application.connections()).hasSize(1);
    }

    @Test
    void disconnecting_an_api_that_was_never_connected_changes_nothing() {
        RegisteredApplication application = created("olive").application();

        service.disconnect("olive", application.id().toString(), "hearing-results");

        verify(apim, never()).unsubscribe(any());
    }

    @Test
    void deleting_should_take_back_every_subscription_and_then_the_entra_application() {
        RegisteredApplication application = created("olive").application();
        when(apim.subscribe("My App", "product-1"))
            .thenReturn(new ApimClient.Subscription("product-1", "my-app-1", "key-1"));
        when(apim.subscribe("My App", "product-2"))
            .thenReturn(new ApimClient.Subscription("product-2", "my-app-2", "key-2"));
        service.connect("olive", application.id().toString(), "hearing-results");
        service.connect("olive", application.id().toString(), "court-listings");

        service.delete("olive", application.id().toString());

        InOrder order = inOrder(apim, entra);
        order.verify(apim).unsubscribe("my-app-1");
        order.verify(apim).unsubscribe("my-app-2");
        order.verify(entra).delete("client-1");
        assertThat(service.list("olive")).isEmpty();
    }

    @Test
    void an_application_entra_will_not_delete_should_be_kept_so_it_can_be_tried_again() {
        RegisteredApplication application = created("olive").application();
        doThrow(new IllegalStateException("graph is down")).when(entra).delete("client-1");

        assertThatThrownBy(() -> service.delete("olive", application.id().toString()))
            .isInstanceOf(IllegalStateException.class);

        assertThat(service.list("olive")).containsExactly(application);
    }

    @Test
    void the_apis_on_offer_should_be_listed_in_order() {
        assertThat(service.apis()).containsExactly("court-listings", "hearing-results");
    }
}
