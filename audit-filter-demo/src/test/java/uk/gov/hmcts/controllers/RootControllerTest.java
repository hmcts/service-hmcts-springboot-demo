package uk.gov.hmcts.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RootControllerTest {

    @InjectMocks
    RootController rootController;

    @Test
    void getting_case_document_should_return_case_id_and_document_id() {
        final UUID caseId     = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        final UUID documentId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        final ResponseEntity<Map<String, UUID>> response = rootController.getCaseDocument(caseId, documentId);

        assertThat(response.getBody()).containsEntry("caseId", caseId);
        assertThat(response.getBody()).containsEntry("documentId", documentId);
    }
}
