package uk.gov.hmcts.marketplace.server.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.marketplace.server.service.NotificationService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = uk.gov.hmcts.marketplace.server.ServerApplication.class)
@AutoConfigureMockMvc
class SubscribeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    NotificationService notificationService;

    @Test
    void subscribe_accepts_name_and_callbackUrl_generates_keyId_secret_returns_them_and_notifies() throws Exception {
        String body = "{\"name\":\"Alice\",\"callbackUrl\":\"http://localhost:8081/callback-notify\"}";
        MvcResult result = mvc.perform(post("/api/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.keyId").exists())
                .andExpect(jsonPath("$.secret").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String keyId = JsonPath.parse(responseBody).read("$.keyId", String.class);
        verify(notificationService).notifyClient(eq("http://localhost:8081/callback-notify"), eq("Alice"), eq(keyId));
    }
}
