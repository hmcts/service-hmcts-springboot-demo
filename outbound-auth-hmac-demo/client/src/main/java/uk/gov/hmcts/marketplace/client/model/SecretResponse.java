package uk.gov.hmcts.marketplace.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecretResponse {

    private String keyId;
    private String secret;
}
