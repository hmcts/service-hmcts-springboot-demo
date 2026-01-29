package uk.gov.hmcts.marketplace.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscribeRequest {

    private String name;
    private String callbackUrl;
    /** If set, use this existing keyId (from a prior GET /api/secret). Client must have the secret before subscribing. */
    private String keyId;
}
