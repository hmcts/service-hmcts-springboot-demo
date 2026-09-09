package uk.gov.hmcts.amp.entra.emulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * secretText is returned only by the create call, exactly as Entra behaves — it cannot be read back later.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppSecret(String id, String displayName, String hint, String secretText) {
}
