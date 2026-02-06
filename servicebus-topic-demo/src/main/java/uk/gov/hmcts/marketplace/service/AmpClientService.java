package uk.gov.hmcts.marketplace.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AmpClientService {

    public void receiveMessage(String topicName, String subscriptionName, String message) {
        log.info("AmpClientService received message:{} for {}/{}", message, topicName, subscriptionName);
    }
}
