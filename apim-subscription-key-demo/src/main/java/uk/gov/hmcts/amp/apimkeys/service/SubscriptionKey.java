package uk.gov.hmcts.amp.apimkeys.service;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One APIM subscription, and - only where Azure has been asked for them - its two keys.
 *
 * <p>Listing leaves the keys null, because Azure does not hand them out in a list. See
 * {@link ApimSubscriptionKeyService}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubscriptionKey(String name, String displayName, String scope, String state,
                              String primaryKey, String secondaryKey) {

    public SubscriptionKey withKeys(final String primary, final String secondary) {
        return new SubscriptionKey(name, displayName, scope, state, primary, secondary);
    }
}
