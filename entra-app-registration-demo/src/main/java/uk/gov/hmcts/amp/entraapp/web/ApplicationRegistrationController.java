package uk.gov.hmcts.amp.entraapp.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.amp.entraapp.service.EntraAppRegistrationClient;

@RestController
@RequiredArgsConstructor
@RequestMapping("/applications")
public class ApplicationRegistrationController {

    public record NewApplication(@NotBlank String name) {
    }

    private final EntraAppRegistrationClient registrations;

    /** The only time the client secret is ever available. Entra will not show it again. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntraAppRegistrationClient.Registration register(@Valid @RequestBody final NewApplication request) {
        return registrations.register(request.name());
    }

    @DeleteMapping("/{objectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final String objectId) {
        registrations.delete(objectId);
    }
}
