package uk.gov.hmcts.amp.registration.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.amp.registration.service.RegistrationService;
import uk.gov.hmcts.amp.registration.web.ApplicationViews.ApplicationView;
import uk.gov.hmcts.amp.registration.web.ApplicationViews.ConnectApi;
import uk.gov.hmcts.amp.registration.web.ApplicationViews.CreatedView;
import uk.gov.hmcts.amp.registration.web.ApplicationViews.NewApplication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final RegistrationService registrations;

    public ApplicationController(final RegistrationService registrations) {
        this.registrations = registrations;
    }

    /** Who Entra says is signed in: from the session (a browser) or from the bearer token (a script). */
    @GetMapping("/me")
    public Map<String, Object> me(final Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser user) {
            return Map.of("subject", user.getSubject(), "name", displayName(user.getPreferredUsername(),
                user.getFullName(), user.getSubject()), "via", "Entra sign-in");
        }
        Jwt jwt = (Jwt) principal;
        return Map.of("subject", jwt.getSubject(), "name", displayName(jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("name"), jwt.getSubject()), "via", "bearer token");
    }

    @GetMapping("/apis")
    public List<String> apis() {
        return registrations.apis();
    }

    @GetMapping("/applications")
    public List<ApplicationView> list(final Authentication authentication) {
        return registrations.list(subject(authentication)).stream().map(ApplicationViews::of).toList();
    }

    @PostMapping("/applications")
    public ResponseEntity<CreatedView> create(final Authentication authentication,
                                              @RequestBody final NewApplication request) {
        RegistrationService.Created created = registrations.create(subject(authentication), request.name());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new CreatedView(ApplicationViews.of(created.application()), created.clientSecret()));
    }

    @PostMapping("/applications/{id}/apis")
    public ResponseEntity<ApplicationView> connect(final Authentication authentication,
                                                   @PathVariable final String id,
                                                   @RequestBody final ConnectApi request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationViews.of(
            registrations.connect(subject(authentication), id, request.apiId())));
    }

    @DeleteMapping("/applications/{id}/apis/{apiId}")
    public ApplicationView disconnect(final Authentication authentication, @PathVariable final String id,
                                      @PathVariable final String apiId) {
        return ApplicationViews.of(registrations.disconnect(subject(authentication), id, apiId));
    }

    @DeleteMapping("/applications/{id}")
    public ResponseEntity<Void> delete(final Authentication authentication, @PathVariable final String id) {
        registrations.delete(subject(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private String subject(final Authentication authentication) {
        Object principal = authentication.getPrincipal();
        return principal instanceof OidcUser user ? user.getSubject() : ((Jwt) principal).getSubject();
    }

    private String displayName(final String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "unknown";
    }
}
