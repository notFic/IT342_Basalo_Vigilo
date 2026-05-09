package edu.cit.basalo.vigilo.features.auth;

import edu.cit.basalo.vigilo.features.auth.LoginRequest;
import edu.cit.basalo.vigilo.features.auth.RegisterRequest;
import edu.cit.basalo.vigilo.features.user.User;
import edu.cit.basalo.vigilo.features.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "User registered successfully!"
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
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Login successful!",
                "data", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "role", user.getRole()
                )
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        try {
            String accessToken = payload.get("token");
            if (accessToken == null || accessToken.isBlank()) throw new RuntimeException("Missing Google token");

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(accessToken);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            ResponseEntity<Map> googleResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> userInfo = googleResponse.getBody();
            if (userInfo == null || !userInfo.containsKey("email")) throw new RuntimeException("Invalid Google Token");

            String email = (String) userInfo.get("email");
            String firstName = (String) userInfo.getOrDefault("given_name", "Google");
            String lastName = (String) userInfo.getOrDefault("family_name", "User");

            // Auto-register or authenticate
            User user = userService.findByEmail(email);
            if (user == null) {
                throw new RuntimeException("Unregistered account. Please contact your system administrator.");
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Google Login successful!",
                "data", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "role", user.getRole()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", "Google verification failed: " + e.getMessage()
            ));
        }
    }
}
