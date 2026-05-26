package uk.gov.hmcts.marketplace.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.openapi.api.RootApi;

@RestController
@Slf4j
public class RootController implements RootApi {

    @Override
    public ResponseEntity<String> getRoot() {
        log.info("getRoot called");
        return ResponseEntity.ok("controller-demo is running");
    }
}
