package uk.gov.hmcts.amp.entra.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProtectedController {

    @GetMapping("/hello")
    public Map<String, Object> hello(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "Hello from HMCTS API",
            "subject", jwt.getSubject(),
            "tenantId", jwt.getClaimAsString("tid") != null ? jwt.getClaimAsString("tid") : "n/a",
            "roles", jwt.getClaimAsStringList("roles") != null ? jwt.getClaimAsStringList("roles") : java.util.List.of()
        );
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "sub", jwt.getSubject(),
            "oid", jwt.getClaimAsString("oid") != null ? jwt.getClaimAsString("oid") : "n/a",
            "tid", jwt.getClaimAsString("tid") != null ? jwt.getClaimAsString("tid") : "n/a",
            "issuer", jwt.getIssuer().toString(),
            "expiresAt", jwt.getExpiresAt().toString()
        );
    }
}
