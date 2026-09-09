package uk.gov.hmcts.amp.entra.emulator.model;

public record TenantInfo(String tenantId, String issuer, String tokenEndpoint, String discoveryEndpoint) {
}
