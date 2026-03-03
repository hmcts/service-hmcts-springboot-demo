package uk.gov.hmcts.marketplace.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotateSecretResponse {

    private String keyId;
    private String message;
}
