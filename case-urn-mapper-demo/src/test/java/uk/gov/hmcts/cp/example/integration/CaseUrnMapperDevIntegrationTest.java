package uk.gov.hmcts.cp.example.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.cp.example.config.TrustAllSslRestClientConfig;
import uk.gov.hmcts.cp.example.service.ExampleConsumingService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Runs on Mac only — dev endpoint requires VPN and a trust-all SSL config not safe for CI (Linux).
// Not intended to be copied to repos that interact with urn-mapper just here to show a real example
// Beware ... of course its possible the data on dev may change and the DEV_CASE_URN may stop working
@EnabledOnOs(OS.MAC)
@SpringBootTest(properties = {
        "case-urn-mapper-client.basepath=https://devamp01.ingress01.dev.nl.cjscp.org.uk",
})
@Import(TrustAllSslRestClientConfig.class)
class CaseUrnMapperDevIntegrationTest {

    private final String devCaseUrn = "28DI9045455";
    private final UUID devCaseId = UUID.fromString("d4aa3bf3-adc3-4268-8a90-a18d5852844d");

    @Autowired
    ExampleConsumingService exampleConsumingService;

    @Test
    void getting_case_id_for_known_urn_should_return_mapped_case_id() {
        UUID caseId = exampleConsumingService.getCaseId(devCaseUrn);
        assertThat(caseId).isEqualTo(devCaseId);
    }

    @Test
    void urn_mapper_should_return_400_for_bad_case_urn() {
        final String badCaseUrn = "!!!-invalid-###";
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> exampleConsumingService.getCaseId(badCaseUrn));
        assertThat(exception.getMessage()).contains("Case urn must be between 1 and 30 alphanumerics");
    }

    @Test
    void mapper_should_return_404_for_unknown_case_urn() {
        final String unknownCaseUrn = "UNKNOWN999";
        HttpClientErrorException.NotFound exception = assertThrows(HttpClientErrorException.NotFound.class,
                () -> exampleConsumingService.getCaseId(unknownCaseUrn));
        assertThat(exception.getMessage()).contains("404 Not Found on GET request");
    }
}
