package uk.gov.hmcts.marketplace.integration;

import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.NoHandlerFoundException;
import uk.gov.hmcts.marketplace.services.ExampleService;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Weird that when we pull in openapi interface to controller that we need to add handler for the below
 * exceptions
 * They just work as expected for a default controller !
 * ... to be investigated and fixed up without the need for lots of ExceptionHandlers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Slf4j
class SpringBootValidationIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    ExampleService exampleService;

    Long exampleId = 1234L;


    @Test
    void random_url_should_be_404() throws Exception {
        mockMvc
                .perform(get("/any-old-path"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void empty_uuid_should_be_404() throws Exception {
        mockMvc
                .perform(get("/by-path/{id}", ""))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void bad_uuid_should_be_400() throws Exception {
        mockMvc
                .perform(get("/by-path/{id}", "not-a-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void resource_not_found_should_be_404() throws Exception {
        HttpClientErrorException notFound = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        when(exampleService.getExample(exampleId)).thenThrow(notFound);
        mockMvc
                .perform(get("/by-path/{id}", exampleId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void no_handler_found_should_be_404() throws Exception {
        NoHandlerFoundException e = new NoHandlerFoundException("GET", "any-url", null);
        when(exampleService.getExample(exampleId)).thenThrow(e);
        mockMvc
                .perform(get("/by-path/{id}", exampleId))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void constraint_violation_should_be_400() throws Exception {
        ConstraintViolationException e = new ConstraintViolationException(Set.of());
        when(exampleService.getExample(exampleId)).thenThrow(e);
        mockMvc
                .perform(get("/by-path/{id}", exampleId))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_with_id_should_be_ok() throws Exception {
        String body = "{\"id\":\"not-long\"}";
        mockMvc
                .perform(post("/example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
        // Its 400 with blank body when the openapi throws 500 ??
        // We can get the error from the exception handler ?  HttpMessageNotReadableException: JSON parse error:
        // Cannot deserialize value of type `long` from String \"not-long\": not a valid `long` value]
    }
}