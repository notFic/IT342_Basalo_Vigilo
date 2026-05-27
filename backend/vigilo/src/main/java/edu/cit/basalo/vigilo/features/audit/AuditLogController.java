package edu.cit.basalo.vigilo.features.audit;
import edu.cit.basalo.vigilo.features.audit.AuditLogService;
import edu.cit.basalo.vigilo.features.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {
    private final AuditLogService auditLogService;
    private final UserService userService;
    public AuditLogController(AuditLogService s, UserService userService) {
        this.auditLogService = s;
        this.userService = userService;
    }
    @GetMapping
    public ResponseEntity<?> getLogs(Principal principal) {
        try {
            userService.requireAdminByEmail(principal.getName());
            return ResponseEntity.ok(auditLogService.getLogs());
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        }
    }
}
