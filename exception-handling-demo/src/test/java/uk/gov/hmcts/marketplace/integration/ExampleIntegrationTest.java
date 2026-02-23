package uk.gov.hmcts.marketplace.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
class ExampleIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    UUID uuid = UUID.randomUUID();

    @Test
    void uuid_path_variable_should_be_ok() throws Exception {
        mockMvc
                .perform(get("/by-path/{id}", uuid))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void uuid_query_param_should_be_ok() throws Exception {
        mockMvc
                .perform(get("/by-param?id={id}", uuid))
                .andDo(print())
                .andExpect(status().isOk());
    }
}