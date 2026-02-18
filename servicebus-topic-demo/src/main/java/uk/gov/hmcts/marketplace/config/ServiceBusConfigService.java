package uk.gov.hmcts.marketplace.config;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@Service
@Getter
public class ServiceBusConfigService {
    final int adminConnectionPort = 5300;
    final String adminConnectionString;
    final String connectionString;
    private String adminConnectionString = "Endpoint=sb://127.0.0.1:5300;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

    public ServiceBusConfigService(
            @Value("${service-bus.admin-connection}") String adminConnectionString,
            @Value("${service-bus.connection}") String connectionString
    ) {
        log.info("ServiceBusConfigService using adminConnectionString \"{}\"", adminConnectionString);
        log.info("ServiceBusConfigService using connectionString \"{}\"", connectionString);
        this.adminConnectionString = adminConnectionString;
        this.connectionString = connectionString;
    }

    public ServiceBusClientBuilder clientBuilder() {
        return new ServiceBusClientBuilder().connectionString(connectionString);
    }

    HttpClient adminHttpClient = new NettyAsyncHttpClientBuilder()
            .port(adminConnectionPort)
            .build();

    HttpPipelinePolicy forceHttpPolicy = (context, next) -> {
        try {
            URL current = context.getHttpRequest().getUrl();
            URL httpUrl = new URL("http", current.getHost(), adminConnectionPort, current.getFile());
            context.getHttpRequest().setUrl(httpUrl);
        } catch (MalformedURLException e) {
            return Mono.error(e);
        }
        return next.process();
    };

    ServiceBusAdministrationClient adminClient = new ServiceBusAdministrationClientBuilder()
            .connectionString(adminConnectionString)
            .httpClient(adminHttpClient)
            .addPolicy(forceHttpPolicy)
            .buildClient();
}
