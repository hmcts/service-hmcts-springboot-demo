package uk.gov.hmcts.marketplace.integration;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;

@Slf4j
@Component
public class ServiceBusTestService {

    private static final int ADMIN_PORT = 5300;
    private static final String ADMIN_CONNECTION_STRING =
            "Endpoint=sb://127.0.0.1:5300;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

    private final ServiceBusAdministrationClient adminClient;

    public ServiceBusTestService() {
        HttpClient httpClient = new NettyAsyncHttpClientBuilder()
                .port(ADMIN_PORT)
                .build();

        HttpPipelinePolicy forceHttpPolicy = (context, next) -> {
            try {
                URL current = context.getHttpRequest().getUrl();
                URL httpUrl = new URL("http", current.getHost(), ADMIN_PORT, current.getFile());
                context.getHttpRequest().setUrl(httpUrl);
            } catch (MalformedURLException e) {
                return Mono.error(e);
            }
            return next.process();
        };

        this.adminClient = new ServiceBusAdministrationClientBuilder()
                .connectionString(ADMIN_CONNECTION_STRING)
                .httpClient(httpClient)
                .addPolicy(forceHttpPolicy)
                .buildClient();
    }

    public void dropQueueIfExists(String queueName) {
        try {
            adminClient.deleteQueue(queueName);
            log.info("Dropped queue {}", queueName);
        } catch (Exception e) {
            log.info("Queue {} does not exist, skipping drop", queueName);
        }
    }
}
