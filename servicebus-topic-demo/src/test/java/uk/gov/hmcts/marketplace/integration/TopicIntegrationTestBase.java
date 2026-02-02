package uk.gov.hmcts.marketplace.integration;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.TopicProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;
import uk.gov.hmcts.marketplace.service.TopicService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class TopicIntegrationTestBase {

    @Autowired
    ServiceBusConfigService configService;
    @Autowired
    TopicService topicService;
    private int adminConnectionPort = 5300;
    private String adminConnectionString = "Endpoint=sb://127.0.0.1:5300;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

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

    String topicName = "topic.1";
    String subscription1 = "subscription.1";
    String subscription2 = "subscription.2";
    int maxDeliveryCount = 3;
    String message = "My message";

    protected boolean isServiceBusReady() {
        try {
            adminClient.listTopics().stream().map(TopicProperties::getName).toList();
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }

    @SneakyThrows
    public void purgeMessages(String topicName, String subscriptionName) {
        log.info("purgeMessages {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = topicService.processorClientBuilder(topicName, subscriptionName, false, 100);
        processorBuilder.processMessage(context -> purgeMessage(topicName, subscriptionName, context));
        processorBuilder.processError(context -> topicService.handleError(topicName, subscriptionName, context));

        ServiceBusProcessorClient processor = processorBuilder.buildProcessorClient();
        processor.start();
        TimeUnit.MILLISECONDS.sleep(100);
        processor.stop();

        log.info("purgeMessages DLQ {}/{}", topicName, subscriptionName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorDlqBuilder = topicService.processorClientBuilder(topicName, subscriptionName, false, 100);
        processorDlqBuilder.processMessage(context -> purgeMessage(topicName, subscriptionName, context));
        processorDlqBuilder.processError(context -> topicService.handleError(topicName, subscriptionName, context));

        ServiceBusProcessorClient processorDlq = processorDlqBuilder.buildProcessorClient();
        processorDlq.start();
        TimeUnit.MILLISECONDS.sleep(100);
        processorDlq.stop();
    }

    private void purgeMessage(String topicName, String subscriptionName, ServiceBusReceivedMessageContext context) {
        log.info("purgeMessage {}/{} {}", topicName, subscriptionName, context.getMessage().getMessageId());
        // do nothing just let the message complete
    }
}
