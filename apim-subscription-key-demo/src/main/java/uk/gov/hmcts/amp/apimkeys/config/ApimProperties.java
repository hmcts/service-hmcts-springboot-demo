package uk.gov.hmcts.amp.apimkeys.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "apim")
public record ApimProperties(String tenantId, String subscriptionId, String resourceGroup, String serviceName) {
}
