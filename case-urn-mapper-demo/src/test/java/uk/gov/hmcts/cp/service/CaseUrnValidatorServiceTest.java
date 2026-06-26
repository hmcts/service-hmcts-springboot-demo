package uk.gov.hmcts.cp.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

class CaseUrnValidatorServiceTest {

    private final CaseUrnValidatorService validator = new CaseUrnValidatorService();

    @Test
    void validating_alphanumeric_urn_should_pass() {
        assertThatCode(() -> validator.validate("URN123456789")).doesNotThrowAnyException();
    }

    @Test
    void validating_null_urn_should_throw_bad_request() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat400((ResponseStatusException) ex));
    }

    @Test
    void validating_urn_with_spaces_should_throw_bad_request() {
        assertThatThrownBy(() -> validator.validate("invalid urn!!"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat400((ResponseStatusException) ex));
    }

    @Test
    void validating_urn_exceeding_30_chars_should_throw_bad_request() {
        assertThatThrownBy(() -> validator.validate("A".repeat(31)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat400((ResponseStatusException) ex));
    }

    @Test
    void validating_empty_urn_should_throw_bad_request() {
        assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat400((ResponseStatusException) ex));
    }

    private void assertThat400(ResponseStatusException ex) {
        org.assertj.core.api.Assertions.assertThat(ex.getStatusCode()).isEqualTo(BAD_REQUEST);
    }
}
