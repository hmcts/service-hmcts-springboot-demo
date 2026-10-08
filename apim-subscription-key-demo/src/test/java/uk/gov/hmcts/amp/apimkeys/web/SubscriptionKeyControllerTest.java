package uk.gov.hmcts.amp.apimkeys.web;

import jakarta.annotation.Resource;
import com.azure.resourcemanager.apimanagement.ApiManagementManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.amp.apimkeys.service.ApimSubscriptionKeyService;
import uk.gov.hmcts.amp.apimkeys.service.SubscriptionKey;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionKeyControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private ApimSubscriptionKeyService subscriptionKeys;

    // So the context never builds a real Azure client: this test is about the endpoints, not about Azure.
    @MockitoBean
    private ApiManagementManager apiManagementManager;

    @Test
    void posting_a_subscription_should_return_created_with_the_key() throws Exception {
        when(subscriptionKeys.create("team-alpha", "hearing-results", "Team Alpha"))
            .thenReturn(subscription("team-alpha").withKeys("primary-key-value", "secondary-key-value"));

        mockMvc.perform(post("/subscription-keys").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"team-alpha","productId":"hearing-results","displayName":"Team Alpha"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.primaryKey").value("primary-key-value"));
    }

    @Test
    void posting_a_name_azure_would_refuse_should_be_rejected_before_azure_is_called() throws Exception {
        mockMvc.perform(post("/subscription-keys").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Team Alpha!","productId":"hearing-results","displayName":"Team Alpha"}"""))
            .andExpect(status().isBadRequest());

        verify(subscriptionKeys, never()).create(any(), any(), any());
    }

    @Test
    void listing_subscriptions_should_return_them_with_no_key_field() throws Exception {
        when(subscriptionKeys.list()).thenReturn(List.of(subscription("team-alpha")));

        mockMvc.perform(get("/subscription-keys"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("team-alpha"))
            .andExpect(jsonPath("$[0].primaryKey").doesNotExist());
    }

    @Test
    void getting_one_subscription_should_return_its_key() throws Exception {
        when(subscriptionKeys.get("team-alpha"))
            .thenReturn(subscription("team-alpha").withKeys("primary-key-value", "secondary-key-value"));

        mockMvc.perform(get("/subscription-keys/team-alpha"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.primaryKey").value("primary-key-value"));
    }

    @Test
    void deleting_a_subscription_should_return_no_content() throws Exception {
        mockMvc.perform(delete("/subscription-keys/team-alpha"))
            .andExpect(status().isNoContent());

        verify(subscriptionKeys).delete(eq("team-alpha"));
    }

    private SubscriptionKey subscription(final String name) {
        return new SubscriptionKey(name, "Team Alpha", "/products/hearing-results", "active", null, null);
    }
}
