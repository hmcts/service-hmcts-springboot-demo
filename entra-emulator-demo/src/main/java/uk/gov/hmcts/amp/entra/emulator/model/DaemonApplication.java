package uk.gov.hmcts.amp.entra.emulator.model;

import java.util.List;

/**
 * Everything a client credentials caller needs, returned together because the emulator — like Entra —
 * only reveals a secret at the moment it is created.
 */
public record DaemonApplication(String clientId, String appIdUri, String clientSecret, List<String> appRoles) {
}
