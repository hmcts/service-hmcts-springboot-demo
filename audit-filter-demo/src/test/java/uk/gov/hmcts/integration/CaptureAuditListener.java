package uk.gov.hmcts.integration;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
class CaptureAuditListener {

    private final LinkedBlockingQueue<String> received = new LinkedBlockingQueue<>();

    @JmsListener(destination = "jms.topic.auditing.event")
    public void capture(final String message) {
        received.add(message);
    }

    List<String> drain(final int expectedCount, final long waitSeconds) throws InterruptedException {
        final List<String> all = new ArrayList<>();
        for (int i = 0; i < expectedCount; i++) {
            final String msg = received.poll(waitSeconds, TimeUnit.SECONDS);
            if (msg == null) {
                break;
            }
            all.add(msg);
        }
        return all;
    }

    void clear() {
        received.clear();
    }
}
