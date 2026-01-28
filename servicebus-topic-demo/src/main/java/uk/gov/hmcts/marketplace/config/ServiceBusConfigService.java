package uk.gov.hmcts.marketplace.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ServiceBusConfigService {
    final String connectionString;

    public ServiceBusConfigService(@Value("${service-bus.connection}") String connectionString) {
        log.info("ServiceBusConfigService using connectionString \"{}\"", connectionString);
        this.connectionString = connectionString;
    }

    public ServiceBusClientBuilder clientBuilder() {
        return new ServiceBusClientBuilder().connectionString(connectionString);
    }
}
