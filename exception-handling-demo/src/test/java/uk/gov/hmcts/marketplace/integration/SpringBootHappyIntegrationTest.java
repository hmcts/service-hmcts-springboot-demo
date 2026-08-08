package uk.gov.hmcts.marketplace.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
class SpringBootHappyIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    Long id = 220L;

    @Test
    void uuid_path_variable_should_be_ok() throws Exception {
        mockMvc
                .perform(get("/by-path/{id}", id))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void uuid_query_param_should_be_ok() throws Exception {
        mockMvc
                .perform(get("/by-param?id={id}", id))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void post_with_id_should_be_ok() throws Exception {
        String body = "{\"id\":21}";
        mockMvc
                .perform(post("/example")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andDo(print())
                .andExpect(status().isOk());
    }
}