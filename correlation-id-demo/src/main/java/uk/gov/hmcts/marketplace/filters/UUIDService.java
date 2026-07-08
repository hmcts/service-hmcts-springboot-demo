package uk.gov.hmcts.marketplace.filters;

import org.springframework.stereotype.Service;

import java.util.UUID;

// Wraps UUID.randomUUID() so it can be mocked in unit tests
@Service
public class UUIDService {

    public String randomString() {
        return UUID.randomUUID().toString();
    }
}
