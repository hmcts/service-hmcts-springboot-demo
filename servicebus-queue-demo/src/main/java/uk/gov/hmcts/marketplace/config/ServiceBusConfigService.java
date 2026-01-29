package uk.gov.hmcts.marketplace.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ServiceBusConfigService {
    final String connectionString;

    public ServiceBusConfigService(@Value("${service-bus.connection}") String connectionString) {
        log.info("ServiceBusConfigService using connectionString \"{}\"", connectionString);
        this.connectionString = connectionString;
    }

    public ServiceBusSenderClient serviceBusSender(String queueName) {
        return new ServiceBusClientBuilder().connectionString(connectionString)
                .sender()
                .queueName(queueName)
                .buildClient();
    }

    public ServiceBusReceiverClient serviceBusReceiver(String queueName) {
        return new ServiceBusClientBuilder().connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .buildClient();
    }
}
