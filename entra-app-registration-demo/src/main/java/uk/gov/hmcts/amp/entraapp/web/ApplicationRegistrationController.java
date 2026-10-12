package uk.gov.hmcts.amp.entraapp.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.amp.entraapp.service.EntraAppRegistrationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/applications")
public class ApplicationRegistrationController {

    public record NewApplication(@NotBlank String name) {
    }

    public record ApiAccess(@NotBlank String role) {
    }

    private final EntraAppRegistrationService registrations;

    /** The only time the client secret is ever available. Entra will not show it again. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntraAppRegistrationService.Registration register(@Valid @RequestBody final NewApplication request) {
        log.info("Registering application named:{}", request.name());
        return registrations.register(request.name());
    }

    @GetMapping
    public List<EntraAppRegistrationService.Summary> list() {
        log.info("Listing applications");
        return registrations.list();
    }

    @GetMapping("/{clientId}")
    public EntraAppRegistrationService.Detail get(@PathVariable final String clientId) {
        log.info("Reading application clientId:{}", clientId);
        return registrations.get(clientId);
    }

    @PostMapping("/{clientId}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantApiAccess(@PathVariable final String clientId, @Valid @RequestBody final ApiAccess request) {
        log.info("Granting role:{} to application clientId:{}", request.role(), clientId);
        registrations.grantApiAccess(clientId, request.role());
    }

    @DeleteMapping("/{clientId}/roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeApiAccess(@PathVariable final String clientId, @PathVariable final String role) {
        log.info("Revoking role:{} from application clientId:{}", role, clientId);
        registrations.revokeApiAccess(clientId, role);
    }

    @DeleteMapping("/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final String clientId) {
        log.info("Deleting application clientId:{}", clientId);
        registrations.delete(clientId);
    }
}
