package uk.gov.hmcts.marketplace.postgres.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.marketplace.postgres.domain.AnswerResponse;
import uk.gov.hmcts.marketplace.postgres.services.AnswerService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnswerControllerTest {

    @Mock
    AnswerService answerService;

    @InjectMocks
    AnswerController answerController;

    @Test
    void root_controller_should_return_ok() {
        AnswerResponse answerResponse = AnswerResponse.builder().answer("Hello").build();
        when(answerService.getAnswer()).thenReturn(answerResponse);
        ResponseEntity<AnswerResponse> response = answerController.getAnswer();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getAnswer()).isEqualTo("Hello");
    }
}