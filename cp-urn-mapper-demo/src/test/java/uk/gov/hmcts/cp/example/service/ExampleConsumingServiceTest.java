package uk.gov.hmcts.cp.example.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.urnmapper.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnCertificateException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnNotFoundException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;
import uk.gov.hmcts.cp.urnmapper.service.CaseUrnMapperService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExampleConsumingServiceTest {

    private static final String CASE_URN = "28DI5874594";

    @Mock
    CaseUrnMapperService caseUrnMapperService;

    @InjectMocks
    ExampleConsumingService exampleConsumingService;

    @Test
    void getting_case_id_should_return_mapped_response() throws Exception {
        CaseMapperResponse expected = CaseMapperResponse.builder()
                .caseUrn(CASE_URN)
                .caseId("37326f3d-15b2-44b3-820e-a8df199f6f80")
                .build();
        when(caseUrnMapperService.getCaseId(CASE_URN)).thenReturn(expected);

        CaseMapperResponse result = exampleConsumingService.getCaseId(CASE_URN);

        assertThat(result.getCaseUrn()).isEqualTo(CASE_URN);
        assertThat(result.getCaseId()).isEqualTo("37326f3d-15b2-44b3-820e-a8df199f6f80");
    }

    @Test
    void getting_case_id_for_unknown_urn_should_propagate_not_found_exception() throws Exception {
        when(caseUrnMapperService.getCaseId(CASE_URN)).thenThrow(new CaseUrnNotFoundException(CASE_URN));

        assertThatThrownBy(() -> exampleConsumingService.getCaseId(CASE_URN))
                .isInstanceOf(CaseUrnNotFoundException.class);
    }

    @Test
    void getting_case_id_for_invalid_urn_should_propagate_validation_exception() throws Exception {
        when(caseUrnMapperService.getCaseId(CASE_URN)).thenThrow(new CaseUrnValidationException("Case URN must be 1-30 alphanumeric characters"));

        assertThatThrownBy(() -> exampleConsumingService.getCaseId(CASE_URN))
                .isInstanceOf(CaseUrnValidationException.class);
    }

    @Test
    void getting_case_id_with_cert_error_should_propagate_certificate_exception() throws Exception {
        when(caseUrnMapperService.getCaseId(CASE_URN)).thenThrow(new CaseUrnCertificateException(CASE_URN, new Exception("SSL error")));

        assertThatThrownBy(() -> exampleConsumingService.getCaseId(CASE_URN))
                .isInstanceOf(CaseUrnCertificateException.class);
    }
}
