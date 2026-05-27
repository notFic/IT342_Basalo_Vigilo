package edu.cit.basalo.vigilo.features.auth;

import edu.cit.basalo.vigilo.features.user.User;
import edu.cit.basalo.vigilo.features.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final GoogleAuthService googleAuthService;

    public AuthController(UserService userService, JwtService jwtService, GoogleAuthService googleAuthService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.googleAuthService = googleAuthService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(
        Principal principal,
        @Valid @RequestBody RegisterRequest request
    ) {
        try {
            // Support backward compatibility if principal is null but X-Requester-Email might be used in edge cases
            // Since we are moving to JWT, principal should not be null if this was authenticated
            String requesterEmail = principal != null ? principal.getName() : null;
            if (requesterEmail == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            User user = userService.registerUser(request, requesterEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "User registered successfully!",
                    "data", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName(),
                        "role", user.getRole()
                    )
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                "success", false,
                "message", e.getReason()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.authenticate(request);
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful!",
                "data", Map.of(
                    "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName(),
                        "role", user.getRole()
                    ),
                    "accessToken", token
                )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Principal principal) {
        try {
            if (principal == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            User user = userService.requireExistingUser(principal.getName());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User loaded successfully.",
                "data", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "role", user.getRole()
                )
            ));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "success", false,
                "message", exception.getReason()
            ));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleOAuthRequest request) {
        try {
            User user = googleAuthService.authenticateGoogleToken(request.getIdToken());
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Google Login successful!",
                "data", Map.of(
                    "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName(),
                        "role", user.getRole()
                    ),
                    "accessToken", token
                )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}

