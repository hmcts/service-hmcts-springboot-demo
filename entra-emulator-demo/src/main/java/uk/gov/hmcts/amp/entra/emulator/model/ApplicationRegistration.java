package uk.gov.hmcts.amp.entra.emulator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApplicationRegistration(String id,
                                      String displayName,
                                      boolean isConfidential,
                                      String appIdUri,
                                      List<AppRole> appRoles,
                                      List<AppSecret> secrets) {
}
