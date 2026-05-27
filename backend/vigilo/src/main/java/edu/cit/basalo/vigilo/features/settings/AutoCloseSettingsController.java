package edu.cit.basalo.vigilo.features.settings;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/settings/auto-close")
public class AutoCloseSettingsController {

    private final AutoCloseSettingsService service;

    public AutoCloseSettingsController(AutoCloseSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> getSettings(Principal principal) {
        try {
            return ResponseEntity.ok(service.getSettingsForUser(principal.getName()));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(
        Principal principal,
        @RequestBody AutoCloseSettingsRequest request
    ) {
        try {
            return ResponseEntity.ok(service.updateSettings(principal.getName(), request));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }
}
