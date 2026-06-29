package uk.gov.hmcts.cp.urnmapper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.urnmapper.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnNotFoundException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;
import uk.gov.hmcts.cp.urnmapper.service.CaseUrnMapperService;

import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Slf4j
class CaseUrnMapperServiceIntegrationTest {

    private static final String CASE_URN = "28DI5874594";
    private static final String UNKNOWN_CASE_URN = "UNKNOWN999";
    private static final String INVALID_CASE_URN = "invalid urn!!";
    private static final String RESPONSES_FOLDER = "responses/";

    private WireMockServer wireMockServer;

    @Autowired
    CaseUrnMapperService caseUrnMapperService;

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

    @SneakyThrows
    @Test
    void getting_case_id_by_case_urn_should_return_mapped_case_id() {
        stubFor(WireMock.get(urlEqualTo("/urnmapper/" + CASE_URN + "?refresh=false"))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readFile("cp-urn-mapper-response.json"))));

        CaseMapperResponse response = caseUrnMapperService.getCaseId(CASE_URN);

        assertThat(response.getCaseUrn()).isEqualTo(CASE_URN);
        assertThat(response.getCaseId()).isEqualTo("37326f3d-15b2-44b3-820e-a8df199f6f80");
    }

    @Test
    void getting_case_id_for_unknown_urn_should_throw_not_found_exception() {
        stubFor(WireMock.get(urlEqualTo("/urnmapper/" + UNKNOWN_CASE_URN + "?refresh=false"))
                .willReturn(aResponse().withStatus(HTTP_NOT_FOUND)));

        assertThatThrownBy(() -> caseUrnMapperService.getCaseId(UNKNOWN_CASE_URN))
                .isInstanceOf(CaseUrnNotFoundException.class)
                .hasMessageContaining(UNKNOWN_CASE_URN);
    }

    @Test
    void getting_case_id_for_invalid_urn_should_throw_validation_exception() {
        assertThatThrownBy(() -> caseUrnMapperService.getCaseId(INVALID_CASE_URN))
                .isInstanceOf(CaseUrnValidationException.class);
    }

    @SneakyThrows
    private String readFile(String filename) {
        return new ClassPathResource(RESPONSES_FOLDER + filename).getContentAsString(StandardCharsets.UTF_8);
    }
}
