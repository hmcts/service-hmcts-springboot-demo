package uk.gov.hmcts.cp.urnmapper.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseUrnValidatorServiceTest {

    private final CaseUrnValidatorService validator = new CaseUrnValidatorService();

    @Test
    void validating_alphanumeric_urn_should_pass() {
        assertThatCode(() -> validator.validate("URN123456789")).doesNotThrowAnyException();
    }

    @Test
    void validating_null_urn_should_throw_validation_exception() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(CaseUrnValidationException.class);
    }

    @Test
    void validating_urn_with_special_chars_should_throw_validation_exception() {
        assertThatThrownBy(() -> validator.validate("invalid urn!!"))
                .isInstanceOf(CaseUrnValidationException.class);
    }

    @Test
    void validating_urn_exceeding_30_chars_should_throw_validation_exception() {
        assertThatThrownBy(() -> validator.validate("A".repeat(31)))
                .isInstanceOf(CaseUrnValidationException.class);
    }

    @Test
    void validating_empty_urn_should_throw_validation_exception() {
        assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(CaseUrnValidationException.class);
    }
}
