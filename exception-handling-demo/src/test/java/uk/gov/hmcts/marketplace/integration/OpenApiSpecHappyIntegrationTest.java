package uk.gov.hmcts.marketplace.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
class OpenApiSpecHappyIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @Test
    void happy_path_should_be_ok() throws Exception {
        mockMvc
                .perform(get("/example/123"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}