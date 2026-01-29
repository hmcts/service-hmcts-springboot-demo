package uk.gov.hmcts.marketplace.client.exception;

/**
 * Thrown when the server returns 409 Conflict (e.g. duplicate subscription for the same name).
 */
public class SubscriptionConflictException extends RuntimeException {

    public SubscriptionConflictException(String message) {
        super(message);
    }
}
