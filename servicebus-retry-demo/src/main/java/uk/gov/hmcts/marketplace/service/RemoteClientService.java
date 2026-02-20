package uk.gov.hmcts.marketplace.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * We emulate a remote service that receives our messages
 * We need to mock sometimes success and sometimes failure
 * We will do this with spring boot properties that wre can override in our int tests
 */
@Service
@AllArgsConstructor
@Slf4j
public class RemoteClientService {

    public int receiveMessage(String topicName, String subscriptionName, String message) {
        log.info("RemoteClientService received message:{} for {}/{}", message, topicName, subscriptionName);
        return 200;
    }
}
