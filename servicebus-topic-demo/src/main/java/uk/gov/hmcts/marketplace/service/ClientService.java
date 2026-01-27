package uk.gov.hmcts.marketplace.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ClientService {

    public void receiveMessage(String message) {
        log.info("ClientService received message:{}", message);
    }
}
