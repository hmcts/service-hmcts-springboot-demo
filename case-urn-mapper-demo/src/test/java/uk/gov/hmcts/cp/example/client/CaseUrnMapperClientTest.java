package uk.gov.hmcts.cp.example.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.cp.example.config.AppProperties;
import uk.gov.hmcts.cp.openapi.model.CaseMapperResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseUrnMapperClientTest {
    @Mock
    private RestClient restClient;
    @Mock
    private AppProperties appProperties;
    @Mock
    RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private CaseUrnMapperClient caseUrnMapperClient;

    private final String mockUrl = "http://mock-server/mapper";
    private final String caseUrn = "28DI5874594";
    private final UUID caseId = UUID.fromString("7a2e94c4-38af-43dd-906b-40d632d159b0");
    private final CaseMapperResponse expectedResponse = new CaseMapperResponse().caseUrn(caseUrn).caseId(caseId);

    @Test
    void getting_case_id_should_return_mapped_response() {
        lenient().when(appProperties.getCaseUrnMapperBasePath()).thenReturn(mockUrl);
        stubRestClient();
        CaseMapperResponse response = caseUrnMapperClient.getCaseMapping(caseUrn);
        assertThat(response).isEqualTo(expectedResponse);
    }

    private void stubRestClient() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(CaseMapperResponse.class)).thenReturn(expectedResponse);
    }
}