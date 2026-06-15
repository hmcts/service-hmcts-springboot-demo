package uk.gov.hmcts.marketplace.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.marketplace.model.FeatureFlagStatus;
import uk.gov.hmcts.marketplace.services.FeatureFlagService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hits real Azure App Config — requires az login against azure hmctsnonlive
 * <p>
 * Run az login to setyp azure credentials
 * We dont run in pipeline, its just here to demonstrate the pattern to developers
 */
@SpringBootTest
class FeatureFlagServiceIntegrationTest {

    @Autowired
    FeatureFlagService featureFlagService;

    String ste30 = "STE30";
    String dev01 = "DEV01";

    @Test
    void ste30_feature_flag_should_return_false() {
        FeatureFlagStatus status = featureFlagService.getStatus("hearingResultsDocumentSubscriptionEnabled", ste30);
        assertThat(status.enabled()).isFalse();
    }

    @Test
    void dev01_feature_flag_should_return_true() {
        FeatureFlagStatus status = featureFlagService.getStatus("hearingResultsDocumentSubscriptionEnabled", dev01);
        assertThat(status.enabled()).isTrue();
    }
}
