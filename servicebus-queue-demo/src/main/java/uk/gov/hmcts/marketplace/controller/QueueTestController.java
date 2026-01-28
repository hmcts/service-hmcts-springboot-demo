package uk.gov.hmcts.marketplace.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import uk.gov.hmcts.marketplace.service.AmpServiceBus;

import java.util.List;
import java.util.Map;

/**
 * Simple REST controller for testing Service Bus queue operations.
 */
@RestController
@RequestMapping("/test/queue")
@RequiredArgsConstructor
@Slf4j
public class QueueTestController {

    private final AmpServiceBus ampServiceBus;

    /**
     * POST /test/queue/send
     * Body: {"message": "Hello World"}
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message is required"));
        }
        log.info("Sending message to queue: {}", message);
        ampServiceBus.sendMessage(message);
        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "message", message,
                "queue", AmpServiceBus.QUEUE_NAME
        ));
    }

    /**
     * Receive messages from the queue.
     * GET /test/queue/receive?maxMessages=10
     */
    @GetMapping("/receive")
    public ResponseEntity<Map<String, Object>> receiveMessages(
            @RequestParam(defaultValue = "10") int maxMessages) {

        log.info("Receiving up to {} messages from queue", maxMessages);
        List<String> messages = ampServiceBus.getMessages(maxMessages);

        return ResponseEntity.ok(Map.of(
                "count", messages.size(),
                "messages", messages,
                "queue", AmpServiceBus.QUEUE_NAME
        ));
    }

    /**
     * Send and receive in one call (for quick testing).
     * POST /test/queue/send-and-receive
     * Body: {"message": "Test Message"}
     */
    @PostMapping("/send-and-receive")
    public ResponseEntity<Map<String, Object>> sendAndReceive(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message is required"));
        }
        log.info("Sending message: {}", message);
        ampServiceBus.sendMessage(message);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Receiving messages");
        List<String> receivedMessages = ampServiceBus.getMessages(10);

        return ResponseEntity.ok(Map.of(
                "sent", message,
                "received", receivedMessages,
                "count", receivedMessages.size(),
                "queue", AmpServiceBus.QUEUE_NAME
        ));
    }

    /**
     * Health check endpoint.
     * GET /test/queue/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "queue", AmpServiceBus.QUEUE_NAME
        ));
    }
}

