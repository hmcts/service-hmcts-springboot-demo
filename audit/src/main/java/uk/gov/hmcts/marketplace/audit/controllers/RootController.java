package uk.gov.hmcts.marketplace.audit.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@AllArgsConstructor
@Slf4j
public class RootController {

    @GetMapping("/")
    public ResponseEntity<String> root() {
        log.info("root controller invoked");
        return ok("Hello world");
    }
}
