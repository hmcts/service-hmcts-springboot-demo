package uk.gov.hmcts.marketplace.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.server.model.NotifyRequest;
import uk.gov.hmcts.marketplace.server.model.NotifyResponse;
import uk.gov.hmcts.marketplace.server.model.SubscriberRow;
import uk.gov.hmcts.marketplace.server.service.NotificationService;
import uk.gov.hmcts.marketplace.server.service.SubscriberStore;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotifyController {

    private final SubscriberStore subscriberStore;
    private final NotificationService notificationService;

    /**
     * Sends the given message to all subscribers. Each notification is signed with that subscriber's secret.
     */
    @PostMapping("/notify")
    public ResponseEntity<NotifyResponse> notifyAll(@RequestBody NotifyRequest request) {
        String message = request.getMessage() != null ? request.getMessage() : "";
        Collection<SubscriberRow> rows = subscriberStore.getAllRows();
        int notified = 0;
        for (SubscriberRow row : rows) {
            try {
                notificationService.notifySubscriber(row, message);
                notified++;
            } catch (Exception e) {
                log.warn("Failed to notify subscriber {}: {}", row.getName(), e.getMessage());
            }
        }
        log.info("Notified {} of {} subscribers", notified, rows.size());
        return ResponseEntity.accepted().body(new NotifyResponse(notified));
    }
}
