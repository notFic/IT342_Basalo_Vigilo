package edu.cit.basalo.vigilo.features.auth;

import edu.cit.basalo.vigilo.features.user.User;
import edu.cit.basalo.vigilo.features.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;

    public GoogleAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticateGoogleToken(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            Map<String, Object> payload = response.getBody();
            if (payload != null && payload.containsKey("email")) {
                String email = (String) payload.get("email");
                
                // The SDD mandates that the user must ALREADY exist in the database (pre-registered by Admin)
                return userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Google account email is not pre-registered. Please contact an Administrator."));
            } else {
                throw new RuntimeException("Invalid Google Access token. Email not found.");
            }
        } catch (Exception e) {
            if (e.getMessage().contains("pre-registered")) {
                throw new RuntimeException(e.getMessage());
            }
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }
}
