package uk.gov.hmcts.marketplace.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single row in the subscriber table: name (primary key), keyId, secret, callbackUrl.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberRow {

    private String name;
    private String keyId;
    private String secret;
    private String callbackUrl;
}
