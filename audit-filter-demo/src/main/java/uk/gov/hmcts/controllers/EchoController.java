package uk.gov.hmcts.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EchoController {

    @GetMapping("/")
    public Map<String, String> root(@RequestParam(defaultValue = "world") final String name) {
        return Map.of("message", "hello " + name);
    }

    @PostMapping("/echo")
    public ResponseEntity<?> echo(@RequestBody final Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("received", body));
    }

    @GetMapping("/error")
    public ResponseEntity<?> error() {
        return ResponseEntity.status(418).body(Map.of("error", "teapot"));
    }
}
