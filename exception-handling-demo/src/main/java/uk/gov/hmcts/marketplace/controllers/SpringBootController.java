package uk.gov.hmcts.marketplace.controllers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.services.ExampleService;

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

    @PostMapping("/example")
    public ResponseEntity<Long> post(@RequestBody EmbeddedExample body) throws Exception {
        log.info("Getting byParam for id:{}", body.getId());
        return ResponseEntity.ok(exampleService.getExample(body.getId()));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    static class EmbeddedExample {
        private long id;
    }
}
