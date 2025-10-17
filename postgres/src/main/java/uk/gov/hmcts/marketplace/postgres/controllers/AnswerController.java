package uk.gov.hmcts.marketplace.postgres.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerResponse;
import uk.gov.hmcts.marketplace.postgres.services.AnswerService;

@RestController
@AllArgsConstructor
public class AnswerController {

    private final AnswerService service;

    public ResponseEntity<AnswerResponse> getAnswer() {
        return ResponseEntity.ok(service.getAnswer());
    }

}
