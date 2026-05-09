package edu.cit.basalo.vigilo.service;
import edu.cit.basalo.vigilo.entity.AuditLog;
import edu.cit.basalo.vigilo.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditLogService {
    private final AuditLogRepository repo;
    public AuditLogService(AuditLogRepository repo) { this.repo = repo; }
    public void logEvent(String email, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUserEmail(email);
        log.setActionPerformed(action);
        log.setDetails(details);
        repo.save(log);
    }
    public List<AuditLog> getLogs() { return repo.findAllByOrderByTimestampDesc(); }
}
