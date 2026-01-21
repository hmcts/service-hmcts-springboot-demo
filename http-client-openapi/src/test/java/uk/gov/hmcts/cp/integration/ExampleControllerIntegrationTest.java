package uk.gov.hmcts.cp.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.cp.service.OpenApiConsumerService;
import uk.gov.hmcts.cp.openapi.model.ExampleResponse;

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

    @Test
    void client_should_get_example() {
        ResponseDefinitionBuilder mockResponse = aResponse()
                .withStatus(HTTP_OK)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"exampleId\":\"21\",\"exampleText\":\"Hello\"}".getBytes());
        stubFor(WireMock.get(urlEqualTo("/example/" + 21)).willReturn(mockResponse));

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
}