package uk.gov.hmcts.amp.entraapp.config;

/** The one setting with no default. Its own type so a FailureAnalyzer can report it plainly. */
public class MissingOnboardingSecretException extends RuntimeException {

    public MissingOnboardingSecretException() {
        super("ENTRA_ONBOARDING_CLIENT_SECRET is not set");
    }
}
