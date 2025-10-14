package uk.gov.hmcts.marketplace.postgres.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@AutoConfigureMockMvc
@Slf4j
class AuditFilterIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @Test
    void root_endpoint_should_return_ok_when_filter_disabled() throws Exception {
        mockMvc
                .perform(
                        get("/")
                                .header("skip-audit-filter", true))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello world"));
    }

    @Test
    void root_endpoint_with_audit_filter_sadly_fails() throws Exception {
        mockMvc
                .perform(
                        get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello world"));
    }
}