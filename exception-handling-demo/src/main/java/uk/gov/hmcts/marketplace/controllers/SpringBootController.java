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
public class SpringBootController {

    ExampleService exampleService;

    @GetMapping("/by-path/{id}")
    public ResponseEntity<Long> getByPath(@PathVariable long id) throws Exception {
        log.info("Getting byPath for id:{}", id);
        return ResponseEntity.ok(exampleService.getExample(id));
    }

    @GetMapping("/by-param")
    public ResponseEntity<Long> getByParam(@RequestParam long id) throws Exception {
        log.info("Getting byParam for id:{}", id);
        return ResponseEntity.ok(exampleService.getExample(id));
    }
}
