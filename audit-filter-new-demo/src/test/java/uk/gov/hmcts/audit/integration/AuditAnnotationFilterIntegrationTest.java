package uk.gov.hmcts.audit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.audit.annotation.AuditDetail;
import uk.gov.hmcts.audit.controller.PingController;

import java.util.UUID;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.audit.filter.AuditFilter.CORRELATION_ID_HEADER;

@SpringBootTest
@AutoConfigureMockMvc
class AuditAnnotationFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void calling_noannotation_endpoint_should_block_with_403() throws Exception {
        mockMvc.perform(get("/noannotation"))
            .andExpect(status().isForbidden())
            .andExpect(content().string("Audit required"));
    }

    private static final String VALID_PATH =
        "/client-subscriptions/3fa85f64-5717-4562-b3fc-2c963f66afa6/documents/7c9e6679-7425-40de-944b-e07fc1f90ae7";

    @Test
    void calling_valid_endpoint_with_missing_correlation_id_should_block_with_403() throws Exception {
        mockMvc.perform(get(VALID_PATH))
            .andExpect(status().isForbidden())
            .andExpect(content().string("Audit required"));
    }

    @Test
    void calling_valid_endpoint_with_correlation_id_should_proceed_with_200() throws Exception {
        mockMvc.perform(get(VALID_PATH).header(CORRELATION_ID_HEADER, "test-correlation-id-001"))
            .andExpect(status().isOk());
    }

    @Test
    void valid_endpoint_annotation_should_declare_origin_component_eventname_action_and_path_params() throws Exception {
        final Method method = PingController.class.getMethod("valid", UUID.class, UUID.class);
        final AuditDetail annotation = method.getAnnotation(AuditDetail.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.origin()).isEqualTo("hearing-results-document");
        assertThat(annotation.component()).isEqualTo("QUERY_API");
        assertThat(annotation.eventName()).isEqualTo("hearing-results-document.get-document");
        // component is not declared on the annotation — verifying it resolves to the default
        assertThat(annotation.action()).isEqualTo("Download"); // explicit override of default "View"
        assertThat(annotation.pathParams()).containsExactly("clientSubscriptionId", "documentId");
    }

    @Test
    void calling_excluded_endpoint_without_correlation_id_should_proceed_with_200() throws Exception {
        mockMvc.perform(get("/excluded"))
            .andExpect(status().isOk());
    }
}
