package uk.gov.hmcts.cp.storage;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class AzuriteContainerInitialise
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, AfterAllCallback {

    private static final int BLOB_PORT = 10000;

    /**
     * Azurite's well-known, publicly documented development account key.
     * It is NOT a secret — it is hardcoded into every Azurite release.
     * See: https://github.com/Azure/Azurite#default-storage-account
     *
     * <p>In CI pipelines, secret-scanning tools may flag any base64 key in source.
     * To avoid false-positive failures, set a GitHub organisation variable named
     * {@code AZURITE_ACCOUNT_KEY} to this same value so it never appears in source.
     * The variable (not a secret) is read here at runtime; the literal below is
     * used only as a local-development fallback.
     */
    private static final String AZURITE_ACCOUNT_KEY = System.getenv().getOrDefault(
            "AZURITE_ACCOUNT_KEY",
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==");

    private static final GenericContainer<?> azurite =
            new GenericContainer<>("mcr.microsoft.com/azure-storage/azurite:3.35.0")
                    .withExposedPorts(BLOB_PORT)
                    .withCommand("azurite-blob --blobHost 0.0.0.0 --skipApiVersionCheck")
                    .waitingFor(Wait.forLogMessage(".*Azurite Blob service successfully listens.*", 1));

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        azurite.start();

        String connectionString = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                + "AccountKey=" + AZURITE_ACCOUNT_KEY + ";"
                + "BlobEndpoint=http://" + azurite.getHost() + ":" + azurite.getMappedPort(BLOB_PORT) + "/devstoreaccount1;";

        TestPropertyValues.of("azure.storage.connection-string=" + connectionString)
                .applyTo(applicationContext.getEnvironment());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (azurite.isRunning()) {
            azurite.stop();
        }
    }
}
