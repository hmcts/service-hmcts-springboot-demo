package uk.gov.hmcts.marketplace.integration;

import com.azure.core.http.HttpClient;
import com.azure.core.http.netty.NettyAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.QueueProperties;
import com.azure.messaging.servicebus.administration.models.QueueRuntimeProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;
import uk.gov.hmcts.marketplace.service.QueueService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class QueueIntegrationTestBase {

    @Autowired
    ServiceBusConfigService configService;

    @Autowired
    QueueService queueService;

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

    String queueName = "queue.1";
    String message = "My message";

    protected boolean isServiceBusReady() {
        try {
            adminClient.listQueues().stream().map(QueueProperties::getName).toList();
            return true;
        } catch (Exception e) {
            log.info("waiting for servicebus to start");
            return false;
        }
    }

    public int countMessages(String queueName){
        QueueRuntimeProperties properties = adminClient.getQueueRuntimeProperties(queueName);
        return adminClient.getQueueRuntimeProperties(queueName).getActiveMessageCount();
    }

    public int countDeadLetterMessages(String queueName){
        return adminClient.getQueueRuntimeProperties(queueName).getDeadLetterMessageCount();
    }

    @SneakyThrows
    public void purgeMessages(String queueName) {
        log.info("purgeMessages {}", queueName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorBuilder = queueService.processorClientBuilder(queueName, false);
        processorBuilder.processMessage(context -> purgeMessage(queueName, context));
        processorBuilder.processError(context -> queueService.handleError(queueName, context));

        ServiceBusProcessorClient processor = processorBuilder.buildProcessorClient();
        processor.start();
        TimeUnit.MILLISECONDS.sleep(1000);
        processor.stop();

        log.info("purgeMessages DLQ {}", queueName);
        ServiceBusClientBuilder.ServiceBusProcessorClientBuilder processorDlqBuilder = queueService.processorClientBuilder(queueName, true);
        processorDlqBuilder.processMessage(context -> purgeMessage(queueName, context));
        processorDlqBuilder.processError(context -> queueService.handleError(queueName, context));

        ServiceBusProcessorClient processorDlq = processorDlqBuilder.buildProcessorClient();
        processorDlq.start();
        TimeUnit.MILLISECONDS.sleep(1000);
        processorDlq.stop();
    }

    private void purgeMessage(String queueName, ServiceBusReceivedMessageContext context) {
        log.info("purgeMessage {} {}", queueName, context.getMessage().getMessageId());
        // do nothing just let the message complete
    }
}
