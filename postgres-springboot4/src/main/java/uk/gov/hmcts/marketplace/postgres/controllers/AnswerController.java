package uk.gov.hmcts.marketplace.postgres.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerResponse;
import uk.gov.hmcts.marketplace.postgres.services.AnswerService;

@RestController
@AllArgsConstructor
@Slf4j
public class AnswerController {

    private final AnswerService service;

    @GetMapping("/answer/{answerId}")
    @Transactional
    public ResponseEntity<AnswerResponse> getAnswer(@PathVariable long answerId) {
        log.info("Getting answer for answerId:{}", answerId);
        return ResponseEntity.ok(service.getAnswer(answerId));
    }
}
