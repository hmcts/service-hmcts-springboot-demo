package uk.gov.hmcts.marketplace.integration;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.cp.openapi.api.ExamplesApi;
import uk.gov.hmcts.cp.openapi.api.RootApi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SpringBootHappyIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @Test
    void get_root_should_return_ok() throws Exception {
        mockMvc.perform(get(RootApi.PATH_GET_ROOT))
            .andExpect(status().isOk());
    }

    @Test
    void get_example_by_id_should_return_example_response() throws Exception {
        mockMvc.perform(get(ExamplesApi.PATH_GET_EXAMPLE_BY_EXAMPLE_ID, 42L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exampleId").value(42))
            .andExpect(jsonPath("$.exampleText").exists());
    }
}
