package uk.gov.hmcts.marketplace.filters;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDServiceTest {

    private final UUIDService uuidService = new UUIDService();

    @Test
    void generating_random_string_should_return_valid_uuid() {
        final String result = uuidService.randomString();
        assertThat(result).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void generating_two_random_strings_should_return_different_values() {
        assertThat(uuidService.randomString()).isNotEqualTo(uuidService.randomString());
    }
}
