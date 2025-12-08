package uk.gov.hmcts.cp.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
class VersioningIntegrationTest {

    private static final String VERSION_HEADER = "X-API-Version";

    @Resource
    private MockMvc mockMvc;

    @Test
    void default_versioning_should_return_latest() throws Exception {
        MvcResult response = mockMvc.perform(get("/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertThat(response.getResponse().getHeader(VERSION_HEADER)).isEqualTo("2.0.0");
    }

    @Test
    void version1_should_return_correct() throws Exception {
        MvcResult response = mockMvc.perform(get("/")
                        .header(VERSION_HEADER, "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertThat(response.getResponse().getHeader(VERSION_HEADER)).isEqualTo("1.0.0");
    }

    @Test
    void version2_should_return_correct() throws Exception {
        MvcResult response = mockMvc.perform(get("/")
                        .header(VERSION_HEADER, "2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        assertThat(response.getResponse().getHeader(VERSION_HEADER)).isEqualTo("2.0.0");
    }
}
