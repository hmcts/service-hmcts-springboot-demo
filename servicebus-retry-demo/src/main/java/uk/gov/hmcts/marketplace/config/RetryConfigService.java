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
import java.util.List;

@Slf4j
@Service
@Getter
public class RetryConfigService {
    final List<Integer> retryDelaySeconds;

    public RetryConfigService(
            @Value("${service-bus.retry-seconds}") List<Integer> retryDelaySeconds
    ) {
        log.info("RetryConfigService using retryDelaySeconds {}", retryDelaySeconds);
        this.retryDelaySeconds = retryDelaySeconds;
    }
}
