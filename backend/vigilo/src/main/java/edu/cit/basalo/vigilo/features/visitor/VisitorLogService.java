package edu.cit.basalo.vigilo.features.visitor;

import edu.cit.basalo.vigilo.features.visitor.VisitorLog;
import edu.cit.basalo.vigilo.features.visitor.VisitorLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.cit.basalo.vigilo.features.audit.AuditLogService;

@Service
public class VisitorLogService {
    private final AuditLogService auditLogService;

    private static final String ACTIVE_STATUS = "Active";
    private static final String CHECKED_OUT_STATUS = "Checked-Out";
    private static final String VOIDED_STATUS = "Voided";
    private static final String AUTO_CLOSED_STATUS = "Auto-Closed";

    private static final Logger log = LoggerFactory.getLogger(VisitorLogService.class);

    private final VisitorLogRepository visitorLogRepository;

    public VisitorLogService(VisitorLogRepository visitorLogRepository, AuditLogService auditLogService) {
        this.visitorLogRepository = visitorLogRepository;
        this.auditLogService = auditLogService;
    }

    public VisitorLog createVisitorLog(
        String fullName,
        String contactNumber,
        String hostName,
        String visitorType,
        String destinationRoom,
        String purpose,
        boolean extendedVisit,
        String createdByEmail,
        MultipartFile idImage
    ) throws IOException {
        validateCheckIn(fullName, contactNumber, hostName, visitorType, destinationRoom, purpose, createdByEmail, idImage);

        VisitorLog visitorLog = new VisitorLog();
        visitorLog.setFullName(fullName.trim());
        visitorLog.setContactNumber(contactNumber.trim());
        visitorLog.setHostName(hostName.trim());
        visitorLog.setVisitorType(visitorType.trim());
        visitorLog.setDestinationRoom(destinationRoom.trim());
        visitorLog.setPurpose(purpose.trim());
        visitorLog.setExtendedVisit(extendedVisit);
        visitorLog.setCreatedByEmail(createdByEmail.trim());
        visitorLog.setStatus(ACTIVE_STATUS);
        visitorLog.setIdImagePath(storeIdImage(idImage));

        return visitorLogRepository.save(visitorLog);
    }

    public List<VisitorLog> getActiveLogs() {
        return visitorLogRepository.findByStatusOrderByTimeInDesc(ACTIVE_STATUS);
    }

    public VisitorLog checkOutVisitor(Long logId, String updatedByEmail) {
        if (updatedByEmail == null || updatedByEmail.isBlank()) {
            throw new IllegalArgumentException("Updated by email is required.");
        }

        VisitorLog visitorLog = visitorLogRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Visitor log not found."));

        if (!ACTIVE_STATUS.equals(visitorLog.getStatus())) {
            throw new IllegalArgumentException("Only active visitor records can be checked out.");
        }

        visitorLog.setStatus(CHECKED_OUT_STATUS);
        visitorLog.setTimeOut(LocalDateTime.now());
        visitorLog.setUpdatedByEmail(updatedByEmail.trim());

        return visitorLogRepository.save(visitorLog);
    }

    public Page<VisitorLog> getHistoricalLogs(Pageable pageable) {
        return visitorLogRepository.findByStatusNotOrderByTimeInDesc(ACTIVE_STATUS, pageable);
    }

    public VisitorLog voidVisitorLog(Long logId, String updatedByEmail) {
        if (updatedByEmail == null || updatedByEmail.isBlank()) {
            throw new IllegalArgumentException("Updated by email is required.");
        }

        VisitorLog visitorLog = visitorLogRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Visitor log not found."));

        visitorLog.setStatus(VOIDED_STATUS);
        visitorLog.setUpdatedByEmail(updatedByEmail.trim());
        // In a real app, trigger SMTP email here
        log.info("Sending SMTP Notification: Record {} voided by {}", logId, updatedByEmail);
        auditLogService.logEvent(updatedByEmail, "VOID_RECORD", "Voided record ID: " + logId);

        return visitorLogRepository.save(visitorLog);
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 59 23 * * ?") // 11:59 PM daily
    public void autoCloseVisitorLogs() {
        log.info("Running scheduled auto-close job at 11:59 PM");
        List<VisitorLog> activeLogs = visitorLogRepository.findByStatusAndExtendedVisitFalse(ACTIVE_STATUS);
        
        for (VisitorLog logEntry : activeLogs) {
            logEntry.setStatus(AUTO_CLOSED_STATUS);
            logEntry.setTimeOut(LocalDateTime.now().withHour(23).withMinute(59).withSecond(0));
            logEntry.setUpdatedByEmail("SYSTEM");
            visitorLogRepository.save(logEntry);
        }
        log.info("Auto-closed {} non-extended active visitor logs.", activeLogs.size());
        if(activeLogs.size() > 0) auditLogService.logEvent("SYSTEM", "AUTO_CLOSE", "Closed " + activeLogs.size() + " active logs");
    }

    private void validateCheckIn(
        String fullName,
        String contactNumber,
        String hostName,
        String visitorType,
        String destinationRoom,
        String purpose,
        String createdByEmail,
        MultipartFile idImage
    ) {
        if (isBlank(fullName) || isBlank(contactNumber) || isBlank(hostName) || isBlank(visitorType)
            || isBlank(destinationRoom) || isBlank(purpose) || isBlank(createdByEmail)) {
            throw new IllegalArgumentException("All required visitor fields must be provided.");
        }

        if (!contactNumber.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Contact number must contain digits only.");
        }

        if (contactNumber.length() < 7 || contactNumber.length() > 15) {
            throw new IllegalArgumentException("Contact number must be between 7 and 15 digits.");
        }

        if (idImage == null || idImage.isEmpty()) {
            throw new IllegalArgumentException("Visitor ID attachment is required.");
        }
    }

    private String storeIdImage(MultipartFile idImage) throws IOException {
        String originalFilename = idImage.getOriginalFilename();
        String sanitizedName = originalFilename == null ? "id-file" : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = UUID.randomUUID() + "_" + sanitizedName;

        Path uploadDirectory = Paths.get("uploads", "visitor-ids");
        Files.createDirectories(uploadDirectory);

        Path outputPath = uploadDirectory.resolve(fileName);
        Files.copy(idImage.getInputStream(), outputPath, StandardCopyOption.REPLACE_EXISTING);

        return outputPath.toString().replace("\\", "/");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
