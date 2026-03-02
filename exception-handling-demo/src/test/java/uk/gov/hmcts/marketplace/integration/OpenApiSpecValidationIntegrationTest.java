package uk.gov.hmcts.marketplace.integration;

import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.NoHandlerFoundException;
import uk.gov.hmcts.marketplace.services.ExampleService;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
class OpenApiSpecValidationIntegrationTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    ExampleService exampleService;

    Long exampleId = 1234L;

    @Test
    void empty_id_should_be_404() throws Exception {
        mockMvc
                .perform(get("/example/{id}", ""))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void non_long_should_be_400() throws Exception {
        mockMvc
                .perform(get("/example/{id}", "not-a-long"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void no_handler_found_should_be_400() throws Exception {
        NoHandlerFoundException e = new NoHandlerFoundException("GET", "any-url", null);
        when(exampleService.getExample(exampleId)).thenThrow(e);
        mockMvc
                .perform(get("/example/{id}", exampleId))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void constraint_violation_should_be_400() throws Exception {
        ConstraintViolationException e = new ConstraintViolationException(Set.of());
        when(exampleService.getExample(exampleId)).thenThrow(e);
        mockMvc
                .perform(get("/example/{id}", exampleId))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}