package uk.gov.hmcts.amp.apimkeys.service;

import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.apimanagement.ApiManagementManager;
import com.azure.resourcemanager.apimanagement.models.SubscriptionContract;
import com.azure.resourcemanager.apimanagement.models.SubscriptionCreateParameters;
import com.azure.resourcemanager.apimanagement.models.SubscriptionKeysContract;
import com.azure.resourcemanager.apimanagement.models.SubscriptionState;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.amp.apimkeys.config.ApimProperties;

import java.util.List;

/**
 * Creates, lists, reads and deletes Azure API Management subscription keys, through the Azure SDK's
 * management client.
 */
@Service
@RequiredArgsConstructor
public class ApimSubscriptionKeyService {

    private final ApiManagementManager apim;
    private final ApimProperties config;

    public SubscriptionKey create(final String name, final String productId, final String displayName) {
        SubscriptionContract created = apim.subscriptions().createOrUpdate(
                config.resourceGroup(), config.serviceName(), name,
                new SubscriptionCreateParameters()
                        .withScope("/products/" + productId)
                        .withDisplayName(displayName)
                        .withState(SubscriptionState.ACTIVE));
        return withSecrets(asSubscriptionKey(created));
    }

    public List<SubscriptionKey> list() {
        return apim.subscriptions().list(config.resourceGroup(), config.serviceName())
                .stream().map(this::asSubscriptionKey).toList();
    }

    public SubscriptionKey get(final String name) {
        try {
            return withSecrets(asSubscriptionKey(
                    apim.subscriptions().get(config.resourceGroup(), config.serviceName(), name)));
        } catch (final ManagementException e) {
            throw translate(name, e);
        }
    }

    public void delete(final String name) {
        try {
            String matchAnyState = "*";
            apim.subscriptions().delete(config.resourceGroup(), config.serviceName(), name, matchAnyState);
        } catch (final ManagementException e) {
            throw translate(name, e);
        }
    }

    private SubscriptionKey withSecrets(final SubscriptionKey subscription) {
        SubscriptionKeysContract secrets = apim.subscriptions()
                .listSecrets(config.resourceGroup(), config.serviceName(), subscription.name());
        return subscription.withKeys(secrets.primaryKey(), secrets.secondaryKey());
    }

    private SubscriptionKey asSubscriptionKey(final SubscriptionContract subscription) {
        return new SubscriptionKey(subscription.name(), subscription.displayName(), subscription.scope(),
                subscription.state() == null ? null : subscription.state().toString(), null, null);
    }

    private RuntimeException translate(final String name, final ManagementException cause) {
        if (cause.getResponse() != null && cause.getResponse().getStatusCode() == HttpStatus.NOT_FOUND.value()) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "No subscription named " + name, cause);
        }
        return cause;
    }
}
