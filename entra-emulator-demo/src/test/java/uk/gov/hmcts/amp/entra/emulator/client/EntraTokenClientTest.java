package uk.gov.hmcts.amp.entra.emulator.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.amp.entra.emulator.model.AccessToken;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EntraTokenClientTest extends EntraWireMockTestBase {

    private static final String TOKEN_PATH = "/11111111-1111-1111-1111-111111111111/oauth2/v2.0/token";

    @Autowired
    EntraTokenClient tokenClient;

    @BeforeEach
    void resetStubs() {
        WIRE_MOCK.resetAll();
    }

    @Test
    void requesting_a_client_credentials_token_should_return_the_issued_access_token() {
        stubTokenResponse();

        final AccessToken token = tokenClient.clientCredentialsToken("client-id", "client-secret", "api://demo");

        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresIn()).isEqualTo(3600);
        assertThat(token.accessToken()).isEqualTo("header.payload.signature");
    }

    @Test
    void requesting_a_token_for_a_bare_resource_uri_should_append_the_default_scope() {
        stubTokenResponse();

        tokenClient.clientCredentialsToken("client-id", "client-secret", "api://demo");

        WireMock.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                .withRequestBody(containing("grant_type=client_credentials"))
                .withRequestBody(containing("scope=api%3A%2F%2Fdemo%2F.default")));
    }

    @Test
    void requesting_a_token_for_a_scope_already_ending_in_default_should_not_append_it_twice() {
        stubTokenResponse();

        tokenClient.clientCredentialsToken("client-id", "client-secret", "api://demo/.default");

        WireMock.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                .withRequestBody(containing("scope=api%3A%2F%2Fdemo%2F.default")));
    }

    private static void stubTokenResponse() {
        stubFor(WireMock.post(urlEqualTo(TOKEN_PATH))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "token_type": "Bearer",
                                  "expires_in": 3600,
                                  "ext_expires_in": 3600,
                                  "scope": "api://demo/.default",
                                  "access_token": "header.payload.signature"
                                }
                                """)));
    }
}
