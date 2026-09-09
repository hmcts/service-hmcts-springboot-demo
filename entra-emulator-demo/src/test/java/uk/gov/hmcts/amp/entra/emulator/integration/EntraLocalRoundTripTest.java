package uk.gov.hmcts.amp.entra.emulator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.amp.entra.emulator.model.AccessToken;
import uk.gov.hmcts.amp.entra.emulator.model.DaemonApplication;
import uk.gov.hmcts.amp.entra.emulator.model.DirectoryUser;
import uk.gov.hmcts.amp.entra.emulator.service.EntraDirectoryService;
import uk.gov.hmcts.amp.entra.emulator.service.EntraTokenService;

import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the whole demo against a real Entra Local container: register an application,
 * mint its secret, then obtain a client credentials token and inspect the claims.
 */
@SpringBootTest
@Testcontainers
class EntraLocalRoundTripTest {

    private static final String TENANT_ID = "11111111-1111-1111-1111-111111111111";

    @Container
    static final GenericContainer<?> ENTRA_LOCAL =
            new GenericContainer<>(DockerImageName.parse("ghcr.io/cmaneu/entra-local:latest"))
                    .withEnv("TENANT_ID", TENANT_ID)
                    .withExposedPorts(8443)
                    .waitingFor(Wait.forLogMessage(".*Entra Local is listening.*", 1)
                            .withStartupTimeout(Duration.ofMinutes(3)));

    @Autowired
    EntraDirectoryService directoryService;

    @Autowired
    EntraTokenService tokenService;

    @org.springframework.test.context.DynamicPropertySource
    static void entraProperties(final org.springframework.test.context.DynamicPropertyRegistry registry) {
        final String baseUrl = "https://%s:%d".formatted(ENTRA_LOCAL.getHost(), ENTRA_LOCAL.getMappedPort(8443));
        registry.add("entra.base-url", () -> baseUrl);
        registry.add("entra.admin-base-url", () -> baseUrl + "/admin/api");
        registry.add("entra.tenant-id", () -> TENANT_ID);
        registry.add("entra.trust-self-signed", () -> true);
    }

    @Test
    void provisioning_an_application_and_requesting_a_token_should_return_entra_shaped_claims() {
        final DaemonApplication daemon =
                directoryService.provisionDaemonApplication("AMP-1106 Job", "api://amp-1106-job", List.of("app.read"));

        final AccessToken token = tokenService.acquireTokenFor(daemon, daemon.appIdUri());

        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresIn()).isPositive();

        final JsonNode claims = claimsOf(token.accessToken());
        assertThat(claims.get("tid").asText()).isEqualTo(TENANT_ID);
        assertThat(claims.get("aud").asText()).isEqualTo("api://amp-1106-job");
        assertThat(claims.get("azp").asText()).isEqualTo(daemon.clientId());
        assertThat(claims.get("ver").asText()).isEqualTo("2.0");
        assertThat(claims.get("roles").get(0).asText()).isEqualTo("app.read");
    }

    @Test
    void asking_the_emulator_for_its_tenant_should_return_the_configured_tenant_id() {
        assertThat(directoryService.tenant().tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void creating_a_user_should_add_it_to_the_emulator_directory() {
        final DirectoryUser created =
                directoryService.createUser("Colin Test", "colin.test@entralocal.dev", "Password1!");

        assertThat(created.id()).isNotBlank();
        assertThat(directoryService.users())
                .extracting(DirectoryUser::userPrincipalName)
                .contains("colin.test@entralocal.dev");
    }

    @SneakyThrows
    private static JsonNode claimsOf(final String jwt) {
        final String payload = jwt.split("\\.")[1];
        return new ObjectMapper().readTree(Base64.getUrlDecoder().decode(payload));
    }
}
