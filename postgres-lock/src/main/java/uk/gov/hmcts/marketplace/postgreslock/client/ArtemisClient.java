package uk.gov.hmcts.marketplace.postgreslock.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ArtemisClient {

    public void sentToArtemis(String payload) {
        log.info("this will be mocked");
    }
}
