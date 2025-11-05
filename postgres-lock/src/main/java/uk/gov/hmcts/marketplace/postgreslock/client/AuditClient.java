package uk.gov.hmcts.marketplace.postgreslock.client;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@AllArgsConstructor
@Slf4j
public class AuditClient {

    private final ArtemisClient artemisClient;

    @SneakyThrows
    public void sendAudit(String auditPayload) {
        int sleepMsecs = new Random().nextInt(100);
        log.info("dummy send audit to artemis sleeping for ... {} msecs", sleepMsecs);
        artemisClient.sentToArtemis(auditPayload);
        Thread.sleep(sleepMsecs);
    }
}
