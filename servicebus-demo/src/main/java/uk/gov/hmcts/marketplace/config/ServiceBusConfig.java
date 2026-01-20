package uk.gov.hmcts.marketplace.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceBusConfig {

    private static final String CONNECTION_STRING = "Endpoint=sb://localhost;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    private static final String QUEUE_NAME = "queue.1";

    ServiceBusClientBuilder clientBuilder = new ServiceBusClientBuilder()
            .connectionString(CONNECTION_STRING);

    @Bean
    ServiceBusSenderClient senderClient() {
        return clientBuilder
                .sender()
                .queueName(QUEUE_NAME)
                .buildClient();
    }

    @Bean
    ServiceBusReceiverClient receiverClient() {
        return clientBuilder
                .receiver()
                .queueName(QUEUE_NAME)
                .buildClient();
    }
}
