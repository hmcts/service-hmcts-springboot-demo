package uk.gov.hmcts.amp.entraapp.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns the missing secret into Spring Boot's own "APPLICATION FAILED TO START" block, with a
 * description and an action - rather than a stack trace with the reason buried in the middle.
 */
public class MissingOnboardingSecretFailureAnalyzer
        extends AbstractFailureAnalyzer<MissingOnboardingSecretException> {

    @Override
    protected FailureAnalysis analyze(final Throwable rootFailure, final MissingOnboardingSecretException cause) {
        return new FailureAnalysis(
                "ENTRA_ONBOARDING_CLIENT_SECRET is not set, and it is the only setting with no default.",
                String.join(System.lineSeparator(),
                        "Export the amp-client-onboarding secret and start again:",
                        "",
                        "    export ENTRA_ONBOARDING_CLIENT_SECRET=...",
                        "",
                        "This is NOT the sign-in application's secret that entra-node-login-demo uses -"
                                + " that is a different registration, without permission to call Graph."),
                cause);
    }
}
