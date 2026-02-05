package uk.gov.hmcts.cp.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.openapi.model.ExampleResponse;
import uk.gov.hmcts.cp.service.AmpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionMapperTest {

    SubscriptionMapper subscriptionMapper = new SubscriptionMapperImpl();

    @Test
    void mapper_should_set_all_fields() {
        // SHOULD USE BUILDER OF COURSE
        ExampleResponse clientResponse = new ExampleResponse();
        clientResponse.setExampleId(11L);
        clientResponse.setExampleText("Example");
        AmpResponse response = subscriptionMapper.mapClientResponse(clientResponse);
        assertThat(response.getExampleId()).isEqualTo(11L);
        assertThat(response.getExampleText()).isEqualTo("Example");
    }
}