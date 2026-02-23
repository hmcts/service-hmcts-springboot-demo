package uk.gov.hmcts.marketplace.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.services.ExampleService;

import java.util.UUID;

@RestController
@AllArgsConstructor
@Slf4j
public class ExampleController {

    ExampleService exampleService;

    @GetMapping("/by-path/{id}")
    public ResponseEntity<UUID> getByPath(@PathVariable UUID id) throws Exception {
        log.info("Getting byPath for id:{}", id);
        return ResponseEntity.ok(exampleService.example(id));
    }

    @GetMapping("/by-param")
    public ResponseEntity<UUID> getByParam(@RequestParam UUID id) throws Exception {
        log.info("Getting byParam for id:{}", id);
        return ResponseEntity.ok(exampleService.example(id));
    }
}
