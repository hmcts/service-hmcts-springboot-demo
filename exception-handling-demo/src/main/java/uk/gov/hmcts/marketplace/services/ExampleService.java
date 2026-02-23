package uk.gov.hmcts.marketplace.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExampleService {

    public UUID example(UUID id) throws Exception {
        log.info("Service called for id:{}", id);
        return id;
    }
}
