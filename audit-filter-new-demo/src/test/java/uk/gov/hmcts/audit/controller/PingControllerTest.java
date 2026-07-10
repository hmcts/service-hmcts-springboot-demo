package uk.gov.hmcts.audit.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.audit.service.DocumentService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PingControllerTest {

    @Mock private DocumentService documentService;

    @InjectMocks
    private PingController controller;

    @Test
    void valid_should_return_200() {
        final var response = controller.valid(UUID.randomUUID(), UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void excluded_should_return_200() {
        assertThat(controller.excluded().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void no_annotation_should_return_200() {
        assertThat(controller.noAnnotation().getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
