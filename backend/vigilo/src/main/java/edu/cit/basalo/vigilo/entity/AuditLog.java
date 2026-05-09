package edu.cit.basalo.vigilo.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime timestamp;
    private String userEmail;
    private String actionPerformed;
    private String details;

    @PrePersist
    protected void onCreate() { this.timestamp = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getActionPerformed() { return actionPerformed; }
    public void setActionPerformed(String actionPerformed) { this.actionPerformed = actionPerformed; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
