package uk.gov.hmcts.marketplace.services;

import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.marketplace.model.FeatureFlagStatus;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final ConfigurationClient configurationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public FeatureFlagStatus getStatus(String featureName, String label) {
        ConfigurationSetting setting = configurationClient.getConfigurationSetting(".appconfig.featureflag/" + featureName, label);
        if (setting == null) return null;
        JsonNode node = objectMapper.readTree(setting.getValue());
        return new FeatureFlagStatus(node.path("id").asText(), node.path("enabled").asBoolean());
    }
}
