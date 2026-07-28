package uk.gov.hmcts.marketplace.integration;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.marketplace.filters.TracingFilter.CORRELATION_ID_KEY;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TracingIntegrationTest {

    private static final String TEST_CORRELATION_ID = "12345678-1234-1234-1234-123456789012";

    @Resource
    private MockMvc mockMvc;

    @Test
    void request_with_correlation_id_header_should_echo_it_in_response() throws Exception {
        MvcResult result = mockMvc.perform(get("/ping")
                        .header(CORRELATION_ID_KEY, TEST_CORRELATION_ID))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader(CORRELATION_ID_KEY)).isEqualTo(TEST_CORRELATION_ID);
    }

    @Test
    void request_without_correlation_id_header_should_generate_one_in_response() throws Exception {
        MvcResult result = mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader(CORRELATION_ID_KEY)).isNotBlank();
    }
}
