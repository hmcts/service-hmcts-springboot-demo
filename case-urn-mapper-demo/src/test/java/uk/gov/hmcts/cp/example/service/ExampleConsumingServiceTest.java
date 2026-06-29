package uk.gov.hmcts.cp.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.example.client.CaseUrnMapperClient;
import uk.gov.hmcts.cp.openapi.model.CaseMapperResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExampleConsumingServiceTest {

    @Mock
    CaseUrnMapperClient caseUrnMapperClient;

    @InjectMocks
    ExampleConsumingService exampleConsumingService;

    private final String caseUrn = "28DI5874594";
    private final UUID caseId = UUID.fromString("37326f3d-15b2-44b3-820e-a8df199f6f80");
    private final CaseMapperResponse response = new CaseMapperResponse().caseUrn(caseUrn).caseId(caseId);

    @Test
    void getting_case_id_should_return_mapped_response() {
        when(caseUrnMapperClient.getCaseMapping(caseUrn)).thenReturn(response);
        UUID result = exampleConsumingService.getCaseId(caseUrn);
        assertThat(result).isEqualTo(caseId);
    }
}
