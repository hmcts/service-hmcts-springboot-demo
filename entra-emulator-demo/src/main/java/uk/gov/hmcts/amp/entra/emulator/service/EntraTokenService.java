package uk.gov.hmcts.amp.entra.emulator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.amp.entra.emulator.client.EntraTokenClient;
import uk.gov.hmcts.amp.entra.emulator.model.AccessToken;
import uk.gov.hmcts.amp.entra.emulator.model.DaemonApplication;

@Service
@RequiredArgsConstructor
public class EntraTokenService {

    private final EntraTokenClient tokenClient;

    public AccessToken acquireToken(final String clientId, final String clientSecret, final String resourceUri) {
        return tokenClient.clientCredentialsToken(clientId, clientSecret, resourceUri);
    }

    public AccessToken acquireTokenFor(final DaemonApplication application, final String resourceUri) {
        return tokenClient.clientCredentialsToken(application.clientId(), application.clientSecret(), resourceUri);
    }
}
