package uk.gov.hmcts.marketplace.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RootControllerTest {

    @InjectMocks
    RootController rootController;

    @Test
    void calling_root_endpoint_should_return_200_with_deprecated_message() {
        ResponseEntity<String> result = rootController.root();
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo("DEPRECATED root endpoint");
    }
}
