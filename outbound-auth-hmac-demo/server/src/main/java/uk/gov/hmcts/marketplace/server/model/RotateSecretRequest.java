package uk.gov.hmcts.marketplace.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotateSecretRequest {

    private String keyId;
}
