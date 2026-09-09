package uk.gov.hmcts.amp.entra.emulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppRole(String id, String value, String displayName, List<String> allowedMemberTypes, Boolean isEnabled) {

    public static AppRole applicationRole(final String value, final String displayName) {
        return new AppRole(null, value, displayName, List.of("Application"), true);
    }
}
