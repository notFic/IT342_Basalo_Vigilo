package edu.cit.basalo.vigilo.features.user;

import edu.cit.basalo.vigilo.features.user.User;
import edu.cit.basalo.vigilo.features.auth.LoginRequest;
import edu.cit.basalo.vigilo.features.auth.RegisterRequest;
import edu.cit.basalo.vigilo.features.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import edu.cit.basalo.vigilo.features.audit.AuditLogService;

@Service
public class UserService {
    private final edu.cit.basalo.vigilo.features.audit.AuditLogService auditLogService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, edu.cit.basalo.vigilo.features.audit.AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(RegisterRequest request) {
        if (request.getFirstName() == null || request.getFirstName().isBlank()
            || request.getLastName() == null || request.getLastName().isBlank()
            || request.getEmail() == null || request.getEmail().isBlank()
            || request.getPassword() == null || request.getPassword().isBlank()
            || request.getRole() == null || request.getRole().isBlank()) {
            throw new RuntimeException("All registration fields are required.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email address is already registered.");
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName().trim());
        newUser.setLastName(request.getLastName().trim());
        newUser.setEmail(request.getEmail().trim());
        newUser.setRole(request.getRole().trim().toUpperCase());

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        newUser.setPasswordHash(hashedPassword);

        userRepository.save(newUser);
        auditLogService.logEvent(
            newUser.getEmail(),
            "USER_CREATED",
            "Created user account with role " + newUser.getRole()
        );
    }

    public User authenticate(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
            || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Email and password are required.");
        }

        User user = userRepository.findByEmail(request.getEmail().trim())
            .orElseThrow(() -> new RuntimeException("Invalid credentials."));

        String rawPassword = request.getPassword();
        String storedPassword = user.getPasswordHash();

        boolean matches = false;
        if (storedPassword != null && !storedPassword.isBlank()) {
            try {
                matches = passwordEncoder.matches(rawPassword, storedPassword);
            } catch (IllegalArgumentException exception) {
                // Support pre-hash demo data and upgrade it after the first successful login.
                matches = rawPassword.equals(storedPassword);
                if (matches) {
                    user.setPasswordHash(passwordEncoder.encode(rawPassword));
                    userRepository.save(user);
                }
            }
        }

        if (!matches) {
            throw new RuntimeException("Invalid credentials.");
        }

        return user;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
