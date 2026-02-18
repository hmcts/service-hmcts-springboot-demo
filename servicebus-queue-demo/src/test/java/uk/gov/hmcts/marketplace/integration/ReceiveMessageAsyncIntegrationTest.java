// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package uk.gov.hmcts.marketplace.integration;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.Disposable;
import uk.gov.hmcts.marketplace.config.ServiceBusConfigService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@Slf4j
public class ReceiveMessageAsyncIntegrationTest {

    @Autowired
    ServiceBusConfigService configService;

    String queueName = "queue.1";
    String message = "My message";

    @Test
    void doTest() throws InterruptedException {
        run();
    }

    /**
     * This method to invoke this demo on how to receive an {@link ServiceBusReceivedMessage} from an Azure Service Bus
     * Queue.
     *
     * @throws InterruptedException If the program is unable to sleep while waiting for the receive to complete.
     */
    @Test
    public void run() throws InterruptedException {
        AtomicBoolean sampleSuccessful = new AtomicBoolean(true);
        CountDownLatch countdownLatch = new CountDownLatch(1);

        // The connection string value can be obtained by:
        // 1. Going to your Service Bus namespace in Azure Portal.
        // 2. Go to "Shared access policies"
        // 3. Copy the connection string for the "RootManageSharedAccessKey" policy.
        // The 'connectionString' format is shown below.
        // 1. "Endpoint={fully-qualified-namespace};SharedAccessKeyName={policy-name};SharedAccessKey={key}"
        // 2. "<<fully-qualified-namespace>>" will look similar to "{your-namespace}.servicebus.windows.net"
        // 3. "queueName" will be the name of the Service Bus queue instance you created
        //    inside the Service Bus namespace.

        // Create a receiver.
        // Messages are not automatically settled when `disableAutoComplete()` is toggled.
        ServiceBusReceiverAsyncClient receiver = configService
                .clientBuilder()
                .receiver()
                .disableAutoComplete()
                .queueName(queueName)
                .buildAsyncClient();
        log.info("COLING PROC...");

        Disposable subscription = receiver.receiveMessages()
                .flatMap(message -> {
                    log.info("PROCING MESSAGE...");
                    boolean messageProcessed = processMessage(message);

                    // Process the context and its message here.
                    // Change the `messageProcessed` according to you business logic and if you are able to process the
                    // message successfully.
                    // Messages MUST be manually settled because automatic settlement was disabled when creating the
                    // receiver.
                    if (messageProcessed) {
                        log.info("PROCESSED");
                        return receiver.complete(message);
                    } else {
                        log.info("ABANDONED");
                        return receiver.abandon(message);
                    }
                }).subscribe(
                        (ignore) -> System.out.println("Message processed."),
                        error -> sampleSuccessful.set(false)
                );

        // Subscribe is not a blocking call so we wait here so the program does not end.
        countdownLatch.await(10, TimeUnit.SECONDS);

        // Disposing of the subscription will cancel the receive() operation.
        subscription.dispose();

        // Close the receiver.
        receiver.close();

        // This assertion is to ensure that samples are working. Users should remove this.
        Assertions.assertTrue(sampleSuccessful.get());
        log.info("COLING DONE");
    }

    private static boolean processMessage(ServiceBusReceivedMessage message) {
        System.out.printf("Sequence #: %s. Contents: %s%n", message.getSequenceNumber(),
                message.getBody());

        return true;
    }
}
