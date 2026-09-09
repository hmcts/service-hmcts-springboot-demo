package uk.gov.hmcts.amp.entra.emulator.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.amp.entra.emulator.model.AccessToken;
import uk.gov.hmcts.amp.entra.emulator.model.AppRole;
import uk.gov.hmcts.amp.entra.emulator.model.AppSecret;
import uk.gov.hmcts.amp.entra.emulator.model.ApplicationRegistration;
import uk.gov.hmcts.amp.entra.emulator.model.DaemonApplication;
import uk.gov.hmcts.amp.entra.emulator.model.DirectoryUser;
import uk.gov.hmcts.amp.entra.emulator.model.TenantInfo;
import uk.gov.hmcts.amp.entra.emulator.service.EntraDirectoryService;
import uk.gov.hmcts.amp.entra.emulator.service.EntraTokenService;

import java.util.List;

@RestController
@RequestMapping("/api/entra")
@RequiredArgsConstructor
public class EntraDirectoryController {

    private final EntraDirectoryService directoryService;
    private final EntraTokenService tokenService;

    @GetMapping("/tenant")
    public TenantInfo tenant() {
        return directoryService.tenant();
    }

    @GetMapping("/applications")
    public List<ApplicationRegistration> applications() {
        return directoryService.applications();
    }

    @PostMapping("/applications")
    public ApplicationRegistration createApplication(@Valid @RequestBody final CreateApplication request) {
        return directoryService.createApplication(request.displayName(), request.confidential());
    }

    @PostMapping("/applications/{applicationId}/secrets")
    public AppSecret addSecret(@PathVariable final String applicationId, @Valid @RequestBody final CreateSecret request) {
        return directoryService.addSecret(applicationId, request.displayName());
    }

    @PostMapping("/applications/{applicationId}/roles")
    public AppRole addRole(@PathVariable final String applicationId, @Valid @RequestBody final CreateRole request) {
        return directoryService.addApplicationRole(applicationId, request.value(), request.displayName());
    }

    @PostMapping("/daemon-applications")
    public DaemonApplication provisionDaemon(@Valid @RequestBody final ProvisionDaemon request) {
        return directoryService.provisionDaemonApplication(request.displayName(), request.appIdUri(), request.appRoles());
    }

    @GetMapping("/users")
    public List<DirectoryUser> users() {
        return directoryService.users();
    }

    @PostMapping("/users")
    public DirectoryUser createUser(@Valid @RequestBody final CreateUser request) {
        return directoryService.createUser(request.displayName(), request.userPrincipalName(), request.password());
    }

    @PostMapping("/token")
    public AccessToken token(@Valid @RequestBody final TokenRequest request) {
        return tokenService.acquireToken(request.clientId(), request.clientSecret(), request.resourceUri());
    }

    public record CreateApplication(@NotBlank String displayName, boolean confidential) {
    }

    public record CreateSecret(@NotBlank String displayName) {
    }

    public record CreateRole(@NotBlank String value, @NotBlank String displayName) {
    }

    public record ProvisionDaemon(@NotBlank String displayName, @NotBlank String appIdUri, List<String> appRoles) {
    }

    public record CreateUser(@NotBlank String displayName, @NotBlank String userPrincipalName, @NotBlank String password) {
    }

    public record TokenRequest(@NotBlank String clientId, @NotBlank String clientSecret, @NotBlank String resourceUri) {
    }
}
