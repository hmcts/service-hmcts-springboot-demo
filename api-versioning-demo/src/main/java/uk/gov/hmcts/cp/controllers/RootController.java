package uk.gov.hmcts.cp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.http.ResponseEntity.ok;

@RestController
@Slf4j
public class RootController {
    private static final String API_VERSION_HEADER = "X-API-version";

    @GetMapping(value = "/")
    public ResponseEntity<String> root(@RequestHeader(API_VERSION_HEADER) Optional<String> version) {
        String versionString = version.orElse("NOT-SET");
        log.info("Request to / no version {}", version);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(API_VERSION_HEADER, versionString);
        return ok()
                .headers(responseHeaders)
                .body("Hello world");
    }

    @GetMapping(value = "/", headers = "X-API-Version=1")
    public ResponseEntity<String> rootv1(@RequestHeader(API_VERSION_HEADER) String version) {
        log.info("Request to / with version {}", version);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(API_VERSION_HEADER, version);
        return ok()
                .headers(responseHeaders)
                .body("Hello world");
    }

    @GetMapping(value = "/", headers = "X-API-Version=2")
    public ResponseEntity<String> rootv2(@RequestHeader(API_VERSION_HEADER) String version) {
        log.info("Request to / with version {}", version);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(API_VERSION_HEADER, version);
        return ok()
                .headers(responseHeaders)
                .body("Hello world");
    }
}
