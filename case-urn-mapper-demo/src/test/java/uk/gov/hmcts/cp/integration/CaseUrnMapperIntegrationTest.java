package uk.gov.hmcts.cp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class CaseUrnMapperIntegrationTest {

    private static final String CASE_URN = "28DI5874594";
    private static final String UNKNOWN_CASE_URN = "UNKNOWN999";
    private static final String INVALID_CASE_URN = "invalid urn with spaces!!";
    private static final String RESPONSES_FOLDER = "src/test/resources/responses/";

    @LocalServerPort
    int port;

    private WireMockServer wireMockServer;
    private RestClient restClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8090));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
        restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
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
        stubDownstream(urnMapperUrl(CASE_URN), HTTP_OK, "case-urn-mapper-response.json");

        ResponseEntity<CaseMapperResponse> response = restClient.get()
                .uri("/urnmapper/" + CASE_URN)
                .retrieve()
                .toEntity(CaseMapperResponse.class);

        CaseMapperResponse expected = readExpectedResponse("case-urn-mapper-response-expected.json");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCaseUrn()).isEqualTo(expected.getCaseUrn());
        assertThat(response.getBody().getCaseId()).isEqualTo(expected.getCaseId());
    }

    @Test
    void getting_case_id_for_unknown_urn_should_return_404() {
        stubFor(WireMock.get(urlEqualTo(urnMapperUrl(UNKNOWN_CASE_URN)))
                .willReturn(aResponse().withStatus(HTTP_NOT_FOUND)));

        ResponseEntity<String> response = restClient.get()
                .uri("/urnmapper/" + UNKNOWN_CASE_URN)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getting_case_id_for_invalid_urn_should_return_400() {
        ResponseEntity<String> response = restClient.get()
                .uri("/urnmapper/" + INVALID_CASE_URN)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (req, res) -> {})
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String urnMapperUrl(String caseUrn) {
        return "/urnmapper/" + caseUrn + "?refresh=false";
    }

    private void stubDownstream(String url, int status, String filename) {
        ResponseDefinitionBuilder mockResponse = aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(readFile(filename));
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(mockResponse));
    }

    @SneakyThrows
    private String readFile(String filename) {
        return Files.readString(Path.of(RESPONSES_FOLDER + filename));
    }

    @SneakyThrows
    private CaseMapperResponse readExpectedResponse(String filename) {
        return objectMapper.readValue(Path.of(RESPONSES_FOLDER + filename).toFile(), CaseMapperResponse.class);
    }
}
