package uk.gov.hmcts.cp.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.openapi.model.ExampleResponse;
import uk.gov.hmcts.cp.service.OpenApiConsumerService;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Slf4j
class ExampleControllerIntegrationTest {

    private WireMockServer wireMockServer;

    @Autowired
    OpenApiConsumerService openApiConsumerService;

    @SneakyThrows
    @Test
    void client_should_get_example() {
        stubGetResponse("/example/" + 21, "example-response.json");

        ExampleResponse response = openApiConsumerService.getExample(21L);
        assertThat(response.getExampleId()).isEqualTo(21L);
        assertThat(response.getExampleText()).isEqualTo("Hello");
    }

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

    private void stubGetResponse(String url, String filename) {
        ResponseDefinitionBuilder mockResponse = aResponse()
                .withStatus(HTTP_OK)
                .withHeader("Content-Type", "application/json")
                .withBody(readJsonResponseFile(filename));
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(mockResponse));
    }

    @SneakyThrows
    private String readJsonResponseFile(String filename) {
        String folder = "src/test/resources/responses/";
        return Files.readString(Path.of(folder + filename));
    }
}