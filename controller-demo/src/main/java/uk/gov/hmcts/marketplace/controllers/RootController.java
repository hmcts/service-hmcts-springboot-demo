package uk.gov.hmcts.marketplace.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

/**
 * HMCTS Helm charts probe GET / by default. This shim keeps the pod healthy until
 * the probe is updated to use /actuator/health.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ok("DEPRECATED root endpoint");
    }
}
