package uk.gov.hmcts.audit.misc.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UuidService {

    public UUID randomUUID() {
        return UUID.randomUUID();
    }
}
