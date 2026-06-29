package uk.gov.hmcts.cp.example.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.example.client.CaseUrnMapperClient;
import uk.gov.hmcts.cp.openapi.model.CaseMapperResponse;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * We may choose to go with WireMock integration test
 * Or we may mock the RestClient integration test
 * But we dont need both
 */
@SpringBootTest(properties = "case-urn-mapper-client.basepath=http://localhost:8090")
class CaseUrnMapperWireMockIntegrationTest {

    private WireMockServer wireMockServer;

    @Autowired
    CaseUrnMapperClient caseUrnMapperClient;

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8090));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
    }

    @AfterEach
    void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private final String caseUrn = "28DI9045455";
    private final UUID caseId = UUID.fromString("37326f3d-15b2-44b3-820e-a8df199f6f80");

    @SneakyThrows
    @Test
    void getting_case_id_for_known_urn_should_return_mapped_case_id() {
        String body = String.format("{\"caseId\":\"%s\", \"caseUrn\":\"%s\"}", caseId, caseUrn);
        stubFor(WireMock.get(urlEqualTo("/urnmapper/" + caseUrn))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        CaseMapperResponse response = caseUrnMapperClient.getCaseMapping(caseUrn);

        assertThat(response.getCaseUrn()).isEqualTo(caseUrn);
        assertThat(response.getCaseId()).isEqualTo(caseId);
    }
}
