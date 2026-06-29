package uk.gov.hmcts.cp.urnmapper.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import uk.gov.hmcts.cp.openapi.api.CaseUrnMapperApi;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnCertificateException;
import uk.gov.hmcts.cp.urnmapper.mapper.UrnMapperResponseMapper;

import javax.net.ssl.SSLHandshakeException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseUrnMapperServiceTest {

    @Mock
    CaseUrnMapperApi caseUrnMapperApi;

    @Mock
    UrnMapperResponseMapper mapper;

    @Mock
    CaseUrnValidatorService caseUrnValidatorService;

    @InjectMocks
    CaseUrnMapperService caseUrnMapperService;

    @Test
    void calling_backend_with_untrusted_cert_should_throw_certificate_exception() {
        SSLHandshakeException sslCause = new SSLHandshakeException("PKIX path building failed: untrusted root");
        when(caseUrnMapperApi.getCaseIdByCaseUrn(any(), any()))
                .thenThrow(new ResourceAccessException("I/O error", sslCause));

        assertThatThrownBy(() -> caseUrnMapperService.getCaseId("28DI5874594"))
                .isInstanceOf(CaseUrnCertificateException.class)
                .hasMessageContaining("28DI5874594")
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }
}
