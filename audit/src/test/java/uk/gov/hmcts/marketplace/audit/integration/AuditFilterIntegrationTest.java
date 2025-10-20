package uk.gov.hmcts.marketplace.audit.integration;

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
    void root_endpoint_should_return_ok() throws Exception {
        mockMvc
                .perform(
                        get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello world"));
    }
}