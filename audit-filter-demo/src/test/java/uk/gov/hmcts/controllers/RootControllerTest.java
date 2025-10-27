package uk.gov.hmcts.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RootControllerTest {

    @InjectMocks
    RootController rootController;

    @Test
    void root_endpoint_should_return_hello() {
        ResponseEntity<Map<String, String>> response = rootController.root("world");
        assertThat(response.getBody().get("message")).isEqualTo("Hello world");
    }

    @Test
    void echo_endpoint_should_return_request_params() {
        Map<String, Object> request = Map.of("request-param", "request-param-value");
        ResponseEntity<Map<String, Map<String, Object>>> response = rootController.echo(request);
        assertThat(response.getBody().get("received").get("request-param")).isEqualTo("request-param-value");
    }

    @Test
    void error_endpoint_should_return_error() {
        ResponseEntity<Map<String, String>> response = rootController.error();
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("error message");
    }
}