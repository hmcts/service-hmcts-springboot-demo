package uk.gov.hmcts.marketplace.controllers;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.openapi.api.ExamplesApi;
import uk.gov.hmcts.cp.openapi.model.ExampleResponse;
import uk.gov.hmcts.marketplace.services.ExampleService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OpenApiController implements ExamplesApi {

    private final ExampleService exampleService;

    @SneakyThrows
    @Override
    public ResponseEntity<ExampleResponse> getExampleByExampleId(@NotNull final Long exampleId) {
        long responseId = exampleService.getExample(exampleId);
        return ResponseEntity.ok(ExampleResponse.builder().exampleId(responseId).build());
    }

}
