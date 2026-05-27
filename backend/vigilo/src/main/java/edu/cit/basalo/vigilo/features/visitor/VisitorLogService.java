package edu.cit.basalo.vigilo.features.visitor;

import edu.cit.basalo.vigilo.common.PageResponse;
import edu.cit.basalo.vigilo.features.notification.NotificationEmailService;
import edu.cit.basalo.vigilo.features.settings.AutoCloseSettings;
import edu.cit.basalo.vigilo.features.settings.AutoCloseSettingsService;
import edu.cit.basalo.vigilo.features.user.User;
import edu.cit.basalo.vigilo.features.user.UserService;
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
    private final UserService userService;
    private final NotificationEmailService notificationEmailService;
    private final AutoCloseSettingsService autoCloseSettingsService;

    private static final String ACTIVE_STATUS = "Active";
    private static final String CHECKED_OUT_STATUS = "Checked-Out";
    private static final String VOIDED_STATUS = "Voided";
    private static final String AUTO_CLOSED_STATUS = "Auto-Closed";

    private static final Logger log = LoggerFactory.getLogger(VisitorLogService.class);

    private final VisitorLogRepository visitorLogRepository;

    public VisitorLogService(
        VisitorLogRepository visitorLogRepository,
        AuditLogService auditLogService,
        UserService userService,
        NotificationEmailService notificationEmailService,
        AutoCloseSettingsService autoCloseSettingsService
    ) {
        this.visitorLogRepository = visitorLogRepository;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.notificationEmailService = notificationEmailService;
        this.autoCloseSettingsService = autoCloseSettingsService;
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
        User actor = userService.requireExistingUser(createdByEmail);
        validateCheckIn(fullName, contactNumber, hostName, visitorType, destinationRoom, purpose, createdByEmail, idImage);

        VisitorLog visitorLog = new VisitorLog();
        visitorLog.setFullName(fullName.trim());
        visitorLog.setContactNumber(contactNumber.trim());
        visitorLog.setHostName(hostName.trim());
        visitorLog.setVisitorType(visitorType.trim());
        visitorLog.setDestinationRoom(destinationRoom.trim());
        visitorLog.setPurpose(purpose.trim());
        visitorLog.setExtendedVisit(extendedVisit);
        visitorLog.setCreatedByEmail(actor.getEmail());
        visitorLog.setStatus(ACTIVE_STATUS);
        visitorLog.setAutoClosed(Boolean.FALSE);
        visitorLog.setIdImagePath(storeIdImage(idImage));

        VisitorLog savedLog = visitorLogRepository.save(visitorLog);
        auditLogService.logEvent(actor.getEmail(), "CHECK_IN", "Created visitor log ID: " + savedLog.getId());
        return savedLog;
    }

    public List<VisitorLog> getActiveLogs(String requesterEmail) {
        userService.requireExistingUser(requesterEmail);
        return visitorLogRepository.findByStatusOrderByTimeInDesc(ACTIVE_STATUS);
    }

    public VisitorLog checkOutVisitor(Long logId, String updatedByEmail) {
        User actor = userService.requireExistingUser(updatedByEmail);

        VisitorLog visitorLog = visitorLogRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Visitor log not found."));

        if (!ACTIVE_STATUS.equals(visitorLog.getStatus())) {
            throw new IllegalArgumentException("Only active visitor records can be checked out.");
        }

        visitorLog.setStatus(CHECKED_OUT_STATUS);
        visitorLog.setTimeOut(LocalDateTime.now());
        visitorLog.setUpdatedByEmail(actor.getEmail());
        visitorLog.setAutoClosed(Boolean.FALSE);

        VisitorLog savedLog = visitorLogRepository.save(visitorLog);
        auditLogService.logEvent(actor.getEmail(), "CHECK_OUT", "Checked out visitor log ID: " + logId);
        return savedLog;
    }

    public PageResponse<VisitorLog> getHistoricalLogs(Pageable pageable, String requesterEmail, String query) {
        userService.requireExistingUser(requesterEmail);
        Page<VisitorLog> page = isBlank(query)
            ? visitorLogRepository.findByStatusNotOrderByTimeInDesc(ACTIVE_STATUS, pageable)
            : visitorLogRepository.findByStatusNotAndFullNameContainingIgnoreCaseOrderByTimeInDesc(ACTIVE_STATUS, query.trim(), pageable);
        return new PageResponse<>(page);
    }

    public VisitorLog voidVisitorLog(Long logId, String updatedByEmail, String voidReason) {
        User admin = userService.requireAdminByEmail(updatedByEmail);

        VisitorLog visitorLog = visitorLogRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Visitor log not found."));

        if (VOIDED_STATUS.equals(visitorLog.getStatus())) {
            throw new IllegalArgumentException("Visitor record is already voided.");
        }

        visitorLog.setStatus(VOIDED_STATUS);
        visitorLog.setUpdatedByEmail(admin.getEmail());
        visitorLog.setAutoClosed(Boolean.FALSE);

        VisitorLog savedLog = visitorLogRepository.save(visitorLog);
        notificationEmailService.sendVoidNotification(savedLog, admin.getEmail(), voidReason);
        auditLogService.logEvent(
            admin.getEmail(),
            "VOID_RECORD",
            "Voided record ID: " + logId + (isBlank(voidReason) ? "" : " | Reason: " + voidReason.trim())
        );

        return savedLog;
    }

    @org.springframework.scheduling.annotation.Scheduled(
        cron = "${app.auto-close.poll-cron:0 * * * * ?}",
        zone = "${app.auto-close.timezone:Asia/Manila}"
    )
    public void autoCloseVisitorLogs() {
        AutoCloseSettings settings = autoCloseSettingsService.getOrCreateSettings();
        if (settings == null || !settings.isEnabled()) return;

        int staleCount = closeStaleLogs(settings);
        if (staleCount > 0) {
            log.info("Auto-closed {} stale logs from previous days.", staleCount);
        }

        if (autoCloseSettingsService.shouldRunAutoCloseNow(settings)) {
            runAutoCloseJob();
        }
    }

    public int closeStaleLogs(AutoCloseSettings settings) {
        java.time.ZoneId zoneId = java.time.ZoneId.of(settings.getTimezone());
        java.time.LocalDate today = java.time.LocalDate.now(zoneId);
        java.time.LocalTime cutoffTime = autoCloseSettingsService.getCutoffLocalTime(settings);
        
        List<VisitorLog> activeLogs = visitorLogRepository.findByStatusAndExtendedVisitFalse(ACTIVE_STATUS);
        List<VisitorLog> staleLogs = new java.util.ArrayList<>();
        
        for (VisitorLog logEntry : activeLogs) {
            java.time.LocalDate logDate = logEntry.getTimeIn().toLocalDate();
            if (logDate.isBefore(today)) {
                LocalDateTime correctCutoff = logDate.atTime(cutoffTime);
                if (logEntry.getTimeIn().isAfter(correctCutoff)) {
                    correctCutoff = logEntry.getTimeIn();
                }
                logEntry.setStatus(AUTO_CLOSED_STATUS);
                logEntry.setTimeOut(correctCutoff);
                logEntry.setUpdatedByEmail("SYSTEM");
                logEntry.setAutoClosed(Boolean.TRUE);
                staleLogs.add(logEntry);
            }
        }
        
        if (!staleLogs.isEmpty()) {
            visitorLogRepository.saveAll(staleLogs);
            auditLogService.logEvent("SYSTEM", "AUTO_CLOSE_STALE", "Closed " + staleLogs.size() + " stale active logs");
        }
        return staleLogs.size();
    }

    public int runAutoCloseJob() {
        AutoCloseSettings settings = autoCloseSettingsService.getOrCreateSettings();
        log.info("Running auto-close job using timezone {}", settings.getTimezone());
        List<VisitorLog> activeLogs = visitorLogRepository.findByStatusAndExtendedVisitFalse(ACTIVE_STATUS);
        
        java.time.LocalTime cutoffTime = autoCloseSettingsService.getCutoffLocalTime(settings);

        for (VisitorLog logEntry : activeLogs) {
            LocalDateTime timeInLocal = logEntry.getTimeIn();
            java.time.LocalDate logDate = timeInLocal.toLocalDate();
            LocalDateTime correctCutoff = logDate.atTime(cutoffTime);
            
            if (timeInLocal.isAfter(correctCutoff)) {
                correctCutoff = timeInLocal;
            }

            logEntry.setStatus(AUTO_CLOSED_STATUS);
            logEntry.setTimeOut(correctCutoff);
            logEntry.setUpdatedByEmail("SYSTEM");
            logEntry.setAutoClosed(Boolean.TRUE);
        }

        if (!activeLogs.isEmpty()) {
            visitorLogRepository.saveAll(activeLogs);
            auditLogService.logEvent("SYSTEM", "AUTO_CLOSE", "Closed " + activeLogs.size() + " active logs");
        }
        autoCloseSettingsService.markRunCompleted(settings);

        log.info("Auto-closed {} non-extended active visitor logs.", activeLogs.size());
        return activeLogs.size();
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
