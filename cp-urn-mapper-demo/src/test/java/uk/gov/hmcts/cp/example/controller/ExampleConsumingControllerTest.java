package uk.gov.hmcts.cp.example.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.cp.example.exception.GlobalExceptionHandler;
import uk.gov.hmcts.cp.example.service.ExampleConsumingService;
import uk.gov.hmcts.cp.urnmapper.domain.CaseMapperResponse;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnCertificateException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnNotFoundException;
import uk.gov.hmcts.cp.urnmapper.exception.CaseUrnValidationException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExampleConsumingController.class)
@Import(GlobalExceptionHandler.class)
class ExampleConsumingControllerTest {

    private static final String CASE_URN = "28DI5874594";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ExampleConsumingService exampleConsumingService;

    @Test
    void getting_case_id_by_case_urn_should_return_200_with_mapped_response() throws Exception {
        when(exampleConsumingService.getCaseId(CASE_URN)).thenReturn(
                CaseMapperResponse.builder()
                        .caseUrn(CASE_URN)
                        .caseId("37326f3d-15b2-44b3-820e-a8df199f6f80")
                        .build());

        mockMvc.perform(get("/urnmapper/{caseUrn}", CASE_URN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseUrn").value(CASE_URN))
                .andExpect(jsonPath("$.caseId").value("37326f3d-15b2-44b3-820e-a8df199f6f80"));
    }

    @Test
    void getting_case_id_for_unknown_urn_should_return_404() throws Exception {
        when(exampleConsumingService.getCaseId(CASE_URN)).thenThrow(new CaseUrnNotFoundException(CASE_URN));

        mockMvc.perform(get("/urnmapper/{caseUrn}", CASE_URN))
                .andExpect(status().isNotFound());
    }

    @Test
    void getting_case_id_for_invalid_urn_should_return_400() throws Exception {
        when(exampleConsumingService.getCaseId(CASE_URN)).thenThrow(new CaseUrnValidationException("Case URN must be 1-30 alphanumeric characters"));

        mockMvc.perform(get("/urnmapper/{caseUrn}", CASE_URN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getting_case_id_with_cert_error_should_return_502() throws Exception {
        when(exampleConsumingService.getCaseId(CASE_URN)).thenThrow(new CaseUrnCertificateException(CASE_URN, new Exception("SSL error")));

        mockMvc.perform(get("/urnmapper/{caseUrn}", CASE_URN))
                .andExpect(status().isBadGateway());
    }
}
