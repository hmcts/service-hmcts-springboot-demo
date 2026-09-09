package uk.gov.hmcts.amp.entra.emulator.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * One server for the JVM: Spring caches the application context across both client test classes,
 * so a per-class server would leave the cached context pointing at a stopped port.
 */
abstract class EntraWireMockTestBase {

    protected static final WireMockServer WIRE_MOCK =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        WIRE_MOCK.start();
        WireMock.configureFor("localhost", WIRE_MOCK.port());
    }

    @DynamicPropertySource
    static void entraProperties(final DynamicPropertyRegistry registry) {
        registry.add("entra.base-url", WIRE_MOCK::baseUrl);
        registry.add("entra.admin-base-url", () -> WIRE_MOCK.baseUrl() + "/admin/api");
        registry.add("entra.tenant-id", () -> "11111111-1111-1111-1111-111111111111");
        registry.add("entra.trust-self-signed", () -> false);
    }
}
