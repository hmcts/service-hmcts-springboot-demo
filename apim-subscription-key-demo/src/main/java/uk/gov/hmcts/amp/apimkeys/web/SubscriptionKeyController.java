package uk.gov.hmcts.amp.apimkeys.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.amp.apimkeys.service.ApimSubscriptionKeyService;
import uk.gov.hmcts.amp.apimkeys.service.SubscriptionKey;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subscription-keys")
public class SubscriptionKeyController {

    /** Azure's own rule for a subscription name: lower case, letters, digits and hyphens. */
    public record NewSubscriptionKey(
        @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,78}[a-z0-9]") String name,
        @NotBlank String productId,
        @NotBlank String displayName) {
    }

    private final ApimSubscriptionKeyService subscriptionKeys;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionKey create(@Valid @RequestBody final NewSubscriptionKey request) {
        return subscriptionKeys.create(request.name(), request.productId(), request.displayName());
    }

    /** Without the keys: see {@link #get} for one key, deliberately asked for. */
    @GetMapping
    public List<SubscriptionKey> list() {
        return subscriptionKeys.list();
    }

    @GetMapping("/{name}")
    public SubscriptionKey get(@PathVariable final String name) {
        return subscriptionKeys.get(name);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final String name) {
        subscriptionKeys.delete(name);
    }
}
