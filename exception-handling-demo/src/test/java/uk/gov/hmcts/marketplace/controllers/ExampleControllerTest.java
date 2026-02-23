package uk.gov.hmcts.marketplace.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.marketplace.services.ExampleService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExampleControllerTest {

    @Mock
    ExampleService exampleService;

    @InjectMocks
    ExampleController exampleController;

    UUID uuid = UUID.randomUUID();

    @Test
    void uuid_passed_as_path_should_return_ok() throws Exception {
        when(exampleService.example(uuid)).thenReturn(uuid);
        ResponseEntity<UUID> response = exampleController.getByPath(uuid);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(uuid);
    }

    @Test
    void uuid_passed_as_param_should_return_ok() throws Exception {
        when(exampleService.example(uuid)).thenReturn(uuid);
        ResponseEntity<UUID> response = exampleController.getByParam(uuid);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(uuid);
    }
}