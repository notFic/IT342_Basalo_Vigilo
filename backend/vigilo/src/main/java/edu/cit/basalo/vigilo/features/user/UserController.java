package edu.cit.basalo.vigilo.features.user;
import edu.cit.basalo.vigilo.features.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;
    public UserController(UserRepository repo, UserService userService) {
        this.userRepository = repo;
        this.userService = userService;
    }
    @GetMapping
    public ResponseEntity<?> getAllUsers(Principal principal) {
        try {
            userService.requireAdminByEmail(principal.getName());
            return ResponseEntity.ok(userRepository.findAll());
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        }
    }
}
