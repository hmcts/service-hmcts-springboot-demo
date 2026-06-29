package uk.gov.hmcts.cp.example.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.example.service.ExampleConsumingService;
import uk.gov.hmcts.cp.openapi.model.CaseMapperResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * We may choose to go with WireMock integration test
 * Or we may mock the RestClient integration test
 * But we dont need both
 */
@SpringBootTest
class CaseUrnMapperMockedRestClientIntegrationTest {

    @MockitoBean(name = "caseUrnMapperRestClient")
    RestClient restClient;

    @MockitoBean
    RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @MockitoBean
    RestClient.RequestHeadersSpec requestHeadersSpec;

    @MockitoBean
    RestClient.ResponseSpec responseSpec;

    @Autowired
    ExampleConsumingService exampleConsumingService;

    private final String caseUrn = "28DI9045455";
    private final UUID caseId = UUID.fromString("37326f3d-15b2-44b3-820e-a8df199f6f80");

    @Test
    void getting_case_id_should_return_mapped_case_id() {
        stubRestClient();
        UUID result = exampleConsumingService.getCaseId(caseUrn);
        assertThat(result).isEqualTo(caseId);
    }

    private void stubRestClient() {
        CaseMapperResponse mockResponse = new CaseMapperResponse().caseUrn(caseUrn).caseId(caseId);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(CaseMapperResponse.class)).thenReturn(mockResponse);
    }
}
