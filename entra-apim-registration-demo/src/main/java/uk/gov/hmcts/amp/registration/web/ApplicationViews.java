package uk.gov.hmcts.amp.registration.web;

import uk.gov.hmcts.amp.registration.service.RegisteredApplication;

import java.time.Instant;
import java.util.List;

/** What the page sees. The Client Secret appears only in {@link CreatedView}, once. */
public final class ApplicationViews {

    private ApplicationViews() {
    }

    public record SubscriptionView(String apiId, String productId, String subscriptionKey) {
    }

    public record ApplicationView(String id, String name, String clientId, Instant createdAt,
                                  List<SubscriptionView> subscriptions) {
    }

    public record CreatedView(ApplicationView application, String clientSecret) {
    }

    public record NewApplication(String name) {
    }

    public record ConnectApi(String apiId) {
    }

    public static ApplicationView of(final RegisteredApplication application) {
        return new ApplicationView(application.id().toString(), application.name(), application.clientId(),
            application.createdAt(), application.connections().stream()
                .map(c -> new SubscriptionView(c.apiId(), c.subscription().productId(),
                    c.subscription().subscriptionKey()))
                .toList());
    }
}
