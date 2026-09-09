package uk.gov.hmcts.amp.entra.emulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectoryListing<T>(List<T> value) {
}
