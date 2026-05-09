package edu.cit.basalo.vigilo.features.audit;
import edu.cit.basalo.vigilo.features.audit.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {
    private final AuditLogService auditLogService;
    public AuditLogController(AuditLogService s) { this.auditLogService = s; }
    @GetMapping
    public ResponseEntity<?> getLogs() { return ResponseEntity.ok(auditLogService.getLogs()); }
}
