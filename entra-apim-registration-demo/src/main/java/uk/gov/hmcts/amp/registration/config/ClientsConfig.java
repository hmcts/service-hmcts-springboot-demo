package uk.gov.hmcts.amp.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ClientsConfig {

    // One client for the calls out to Entra, Graph and APIM; a bean so a test can swap it.
    //
    // HTTP/1.1 on purpose. The JDK client otherwise offers an upgrade to HTTP/2 on a request with a body,
    // and a plain-HTTP server that does not want one (WireMock, here) answers it and then drops the
    // connection: the caller sees "EOF reached while reading" and the server saw an empty body. Microsoft's
    // endpoints speak HTTP/1.1 perfectly well.
    @Bean
    public RestClient restClient() {
        HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        return RestClient.builder().requestFactory(new JdkClientHttpRequestFactory(http)).build();
    }
}
