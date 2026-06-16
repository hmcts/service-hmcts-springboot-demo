package uk.gov.hmcts.amp.entra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProtectedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void helloRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/hello"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void helloWithValidJwtReturnsMessage() throws Exception {
        mockMvc.perform(get("/api/hello")
                .with(jwt()
                    .jwt(j -> j
                        .subject("test-client")
                        .claim("tid", "test-tenant-id")
                        .claim("roles", java.util.List.of("API.Read")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Hello from HMCTS API"))
            .andExpect(jsonPath("$.subject").value("test-client"))
            .andExpect(jsonPath("$.tenantId").value("test-tenant-id"));
    }

    @Test
    void meEndpointReturnsJwtClaims() throws Exception {
        mockMvc.perform(get("/api/me")
                .with(jwt()
                    .jwt(j -> j
                        .subject("test-client")
                        .claim("tid", "test-tenant-id")
                        .claim("oid", "00000000-0000-0000-0000-000000000001"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sub").value("test-client"))
            .andExpect(jsonPath("$.tid").value("test-tenant-id"));
    }
}
