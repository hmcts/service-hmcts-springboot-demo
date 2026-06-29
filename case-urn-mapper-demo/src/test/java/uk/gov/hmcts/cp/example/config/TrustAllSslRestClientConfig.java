package uk.gov.hmcts.cp.example.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

// Overrides the caseUrnMapperRestClient bean with a trust-all SSL variant.
// In real environments the Dockerfile uses $BASE_IMAGE which includes the CA for the self-signed cert.
@TestConfiguration
@Slf4j
public class TrustAllSslRestClientConfig {

    @Value("${case-urn-mapper-client.basepath}")
    String basePath;

    @Bean
    @Primary
    @SneakyThrows
    RestClient caseUrnMapperRestClient() {
        log.warn("TrustAllSslRestClientConfig: SSL verification disabled — test use only");
        var sslContext = SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build();
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                        .build())
                .build();
        var httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
        return RestClient.builder()
                .baseUrl(basePath)
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
