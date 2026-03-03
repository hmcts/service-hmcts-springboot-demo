package uk.gov.hmcts.marketplace.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = uk.gov.hmcts.marketplace.server.ServerApplication.class)
@AutoConfigureMockMvc
class SecretControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void rotateSecret_returns_new_secret_for_known_keyId() throws Exception {
        String keyId = "kid_v1";
        mvc.perform(post("/api/secret/rotate")
                        .contentType("application/json")
                        .content("{\"keyId\":\"" + keyId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keyId").value(keyId))
                .andExpect(jsonPath("$.secret").exists());
    }
}
