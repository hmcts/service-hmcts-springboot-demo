package uk.gov.hmcts.amp.registration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.amp.registration.apim.ApimClient;
import uk.gov.hmcts.amp.registration.entra.EntraGraphClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The HTTP surface: who may call it, and what comes back. Entra and APIM are mocked; nothing is contacted. */
@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntraGraphClient entra;

    @MockitoBean
    private ApimClient apim;

    private org.springframework.test.web.servlet.request.RequestPostProcessor olive() {
        return jwt().jwt(j -> j.subject("olive").claim("name", "Olive Oyl"));
    }

    private String createApplication() throws Exception {
        when(entra.register("My App")).thenReturn(new EntraGraphClient.Registration("client-1", "s3cret", "key-1"));
        String body = mockMvc.perform(post("/api/applications").with(olive()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"My App\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.clientSecret").value("s3cret"))
            .andExpect(jsonPath("$.application.clientId").value("client-1"))
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.application.id");
    }

    @Test
    void the_page_and_health_are_public() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void the_api_answers_401_not_a_redirect_when_not_signed_in() throws Exception {
        mockMvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_should_say_who_entra_says_is_signed_in() throws Exception {
        mockMvc.perform(get("/api/me").with(olive()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").value("olive"))
            .andExpect(jsonPath("$.name").value("Olive Oyl"))
            .andExpect(jsonPath("$.via").value("bearer token"));
    }

    @Test
    void a_change_from_a_browser_session_needs_the_csrf_token() throws Exception {
        mockMvc.perform(post("/api/applications").with(oidcLogin().idToken(t -> t.subject("olive")))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"My App\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void a_browser_session_signed_in_with_entra_can_register_and_is_told_who_it_is() throws Exception {
        when(entra.register("My App")).thenReturn(new EntraGraphClient.Registration("client-1", "s3cret", "key-1"));
        var session = oidcLogin().idToken(t -> t.subject("olive").claim("preferred_username", "olive@example.com"));

        mockMvc.perform(post("/api/applications").with(session).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"My App\"}"))
            .andExpect(status().isCreated());
        mockMvc.perform(get("/api/me").with(session))
            .andExpect(jsonPath("$.name").value("olive@example.com"))
            .andExpect(jsonPath("$.via").value("Entra sign-in"));
        mockMvc.perform(get("/api/applications").with(session))
            .andExpect(jsonPath("$[0].clientId").value("client-1"));
    }

    @Test
    void registering_connecting_listing_and_deleting_should_work_end_to_end() throws Exception {
        String id = createApplication();
        // The Product is whatever application.yml maps the API to.
        when(apim.subscribe(eq("My App"), anyString()))
            .thenReturn(new ApimClient.Subscription("product-1", "my-app-1", "the-key"));

        mockMvc.perform(post("/api/applications/" + id + "/apis").with(olive()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"apiId\":\"hearing-results\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subscriptions[0].subscriptionKey").value("the-key"));

        // The list shows the Client ID and the keys, never the Client Secret.
        mockMvc.perform(get("/api/applications").with(olive()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].clientId").value("client-1"))
            .andExpect(jsonPath("$[0].clientSecret").doesNotExist())
            .andExpect(jsonPath("$[0].subscriptions[0].apiId").value("hearing-results"));

        mockMvc.perform(delete("/api/applications/" + id + "/apis/hearing-results").with(olive()).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subscriptions").isEmpty());
        mockMvc.perform(delete("/api/applications/" + id).with(olive()).with(csrf()))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/applications").with(olive()))
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void an_api_that_is_not_on_offer_is_refused() throws Exception {
        String id = createApplication();

        mockMvc.perform(post("/api/applications/" + id + "/apis").with(olive()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"apiId\":\"nope\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void another_user_cannot_see_or_change_the_application() throws Exception {
        String id = createApplication();
        var other = jwt().jwt(j -> j.subject("popeye"));

        mockMvc.perform(get("/api/applications").with(other)).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(delete("/api/applications/" + id).with(other).with(csrf()))
            .andExpect(status().isNotFound());
    }

    @Test
    void the_apis_on_offer_are_listed() throws Exception {
        mockMvc.perform(get("/api/apis").with(olive()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("court-listings-and-scheduling"));
    }
}
