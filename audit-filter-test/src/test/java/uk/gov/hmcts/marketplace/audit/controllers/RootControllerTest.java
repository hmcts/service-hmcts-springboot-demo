package uk.gov.hmcts.marketplace.audit.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RootControllerTest {

    @InjectMocks
    RootController rootController;

    @Test
    void root_endpoint_should_return_hello() {
        assertThat(rootController.root().getStatusCode().value()).isEqualTo(200);
        assertThat(rootController.root().getBody()).isEqualTo("Hello");
    }
}