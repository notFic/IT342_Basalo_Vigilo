package edu.cit.basalo.vigilo.service;

import edu.cit.basalo.vigilo.entity.User;
import edu.cit.basalo.vigilo.dto.LoginRequest;
import edu.cit.basalo.vigilo.dto.RegisterRequest;
import edu.cit.basalo.vigilo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final edu.cit.basalo.vigilo.service.AuditLogService auditLogService;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, edu.cit.basalo.vigilo.service.AuditLogService auditLogService) {
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
    }

    public User authenticate(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()
            || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Email and password are required.");
        }

        User user = userRepository.findByEmail(request.getEmail().trim())
            .orElseThrow(() -> new RuntimeException("Invalid credentials."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials.");
        }

        return user;
    }
}
