package uk.gov.hmcts.amp.entra.emulator.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class EntraRestClientConfig {

    @Bean
    RestClient entraRestClient(final EntraProperties properties) {
        final RestClient.Builder builder = RestClient.builder();
        if (properties.trustSelfSigned()) {
            log.warn("entra.trust-self-signed is enabled — the emulator's self-signed certificate will be accepted. "
                    + "Never enable this against a real Entra tenant.");
            builder.requestFactory(new HttpComponentsClientHttpRequestFactory(trustAllHttpClient()));
        }
        return builder.build();
    }

    @SneakyThrows
    private static CloseableHttpClient trustAllHttpClient() {
        final var sslContext = SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build();
        final var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, NoopHostnameVerifier.INSTANCE))
                .build();
        return HttpClients.custom().setConnectionManager(connectionManager).build();
    }
}
