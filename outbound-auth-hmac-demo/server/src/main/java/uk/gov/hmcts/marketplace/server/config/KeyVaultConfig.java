package uk.gov.hmcts.marketplace.server.config;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpPipelineNextSyncPolicy;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;

@Slf4j
@Configuration
public class KeyVaultConfig {

    @Value("${azure.keyvault.url}")
    private String vaultUrl;

    @Value("${azure.keyvault.emulator:true}")
    private boolean emulatorMode;

    @Bean
    public SecretClient secretClient() throws Exception {
        if (emulatorMode) {
            log.warn("Key Vault EMULATOR mode – TLS verification disabled. Not for production.");

            // Workaround -1 - The Azure SDK enforces https for all Key Vault operations where as emulator does not.
            // To meet that need to generate selfsinged certificates at the startup using the files mounted at certs
            // - setup-emulator-certs.sh
            // The JVM normally reject this cert because it's not signed by a trusted CA.
            // Fix: Netty HTTP client with trust-all SSL for Key Vault API calls
            var nettySslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

            HttpClient reactorHttpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(nettySslContext));

            com.azure.core.http.HttpClient azureHttpClient =
                new NettyAsyncHttpClientBuilder(reactorHttpClient).build();

            // Azure SDK uses OAUTH basesd policy and need to do the same for emulator
            // fetch the emulator's own JWT token (GET /token) -- emulator issues its own tokens;
            String tokenUrl = vaultUrl.replaceAll("/+$", "") + "/token";
            log.info("Fetching emulator token from {}", tokenUrl);
            String jwtToken = fetchPlainTextToken(tokenUrl); //Fetches a JWT token via GET using the JDK HttpClient. SSL verification and hostname checking are disabled.
            log.info("Emulator token obtained (length={})", jwtToken.length());

            // Workaround -2 - Emulator is returning jwt token with Bearer header but no claims in it causing ArrayIndexOutOfBoundsException on split operation
            // This is to bypasse KeyVaultCredentialPolicy which fails to parse the emulator's
            // bare 'WWW-Authenticate: Bearer' header (ArrayIndexOutOfBoundsException).
            HttpPipeline pipeline = new HttpPipelineBuilder()
                .httpClient(azureHttpClient)
                .policies(new RetryPolicy(), new StaticBearerTokenPolicy(jwtToken)) // header to every request. Used for the local emulator to avoid the Azure SDK's flow
                    .build();

            log.info("Building emulator SecretClient → vaultUrl={}", vaultUrl);

            return new SecretClientBuilder()
                .vaultUrl(vaultUrl)
                .pipeline(pipeline)   // with custom pipeline; .credential() is ignored when pipeline is set
                .buildClient();
        } else {
            // Production: DefaultAzureCredential
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();
            com.azure.core.http.HttpClient azureHttpClient =
                new NettyAsyncHttpClientBuilder().build();

            log.info("Building production SecretClient → vaultUrl={}", vaultUrl);

            return new SecretClientBuilder()
                .vaultUrl(vaultUrl)
                .credential(credential)
                .httpClient(azureHttpClient)
                .buildClient();
        }
    }

    private static String fetchPlainTextToken(String tokenUrl) throws Exception {
        TrustManager[] trustAll = {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) { }
                public void checkServerTrusted(X509Certificate[] c, String a) { }
            }
        };

        SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(null, trustAll, new SecureRandom());

        SSLParameters sslParams = new SSLParameters();
        sslParams.setEndpointIdentificationAlgorithm(null); // disable hostname verification

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .sslContext(sslCtx)
            .sslParameters(sslParams)
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .GET()
            .build();

        return client.send(request, BodyHandlers.ofString()).body().trim();
    }

    private static final class StaticBearerTokenPolicy implements HttpPipelinePolicy {

        private final String headerValue;

        StaticBearerTokenPolicy(String token) {
            this.headerValue = "Bearer " + token;
        }

        @Override
        public Mono<HttpResponse> process(HttpPipelineCallContext context,
                                          HttpPipelineNextPolicy next) {
            context.getHttpRequest().getHeaders().set("Authorization", headerValue);
            return next.process();
        }

        @Override
        public HttpResponse processSync(HttpPipelineCallContext context,
                                        HttpPipelineNextSyncPolicy next) {
            context.getHttpRequest().getHeaders().set("Authorization", headerValue);
            return next.processSync();
        }
    }
}
