package edu.cit.basalo.vigilo.features.audit;
import edu.cit.basalo.vigilo.features.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc();
}
