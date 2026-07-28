package uk.gov.hmcts.marketplace.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

// Minimal endpoint used to exercise the TracingFilter in integration tests
@RestController
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ok("pong");
    }
}
