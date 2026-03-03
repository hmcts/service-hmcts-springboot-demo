package uk.gov.hmcts.marketplace.client.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.marketplace.client.service.SecretStore;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = uk.gov.hmcts.marketplace.client.ClientApplication.class)
@AutoConfigureMockMvc
class CallbackNotifyControllerTest {

    private static final String KEY_ID = "test-key-id";
    private static final String SECRET = "test-secret";
    private static final String PAYLOAD = "Alice, you have a news";

    @Autowired
    MockMvc mvc;

    @Autowired
    SecretStore secretStore;

    @BeforeEach
    void setUp() {
        secretStore.put(KEY_ID, SECRET);
    }

    @Test
    void callbackNotify_with_valid_hmac_returns_200() throws Exception {
        String signature = computeHmac("POST", "/callback-notify", PAYLOAD, SECRET);
        mvc.perform(post("/callback-notify")
                        .header("X-HMAC-Signature", signature)
                        .header("X-HMAC-Key-Id", KEY_ID)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(PAYLOAD))
                .andExpect(status().isOk());
    }

    @Test
    void callbackNotify_without_hmac_headers_returns_401() throws Exception {
        mvc.perform(post("/callback-notify")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    private String computeHmac(String method, String uri, String body, String secret) throws Exception {
        String payload = method + "\n" + uri + "\n" + (body != null ? body : "");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
