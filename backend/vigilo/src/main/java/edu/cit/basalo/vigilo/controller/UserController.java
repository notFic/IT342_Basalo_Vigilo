package edu.cit.basalo.vigilo.controller;
import edu.cit.basalo.vigilo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository userRepository;
    public UserController(UserRepository repo) { this.userRepository = repo; }
    @GetMapping
    public ResponseEntity<?> getAllUsers() { return ResponseEntity.ok(userRepository.findAll()); }
}
