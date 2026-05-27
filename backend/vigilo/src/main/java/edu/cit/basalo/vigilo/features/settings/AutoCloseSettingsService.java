package edu.cit.basalo.vigilo.features.settings;

import edu.cit.basalo.vigilo.features.audit.AuditLogService;
import edu.cit.basalo.vigilo.features.user.User;
import edu.cit.basalo.vigilo.features.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class AutoCloseSettingsService {

    private final AutoCloseSettingsRepository repository;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final String defaultTimezone;

    public AutoCloseSettingsService(
        AutoCloseSettingsRepository repository,
        UserService userService,
        AuditLogService auditLogService,
        @Value("${app.auto-close.timezone:Asia/Manila}") String defaultTimezone
    ) {
        this.repository = repository;
        this.userService = userService;
        this.auditLogService = auditLogService;
        this.defaultTimezone = defaultTimezone;
    }

    public AutoCloseSettings getSettingsForUser(String requesterEmail) {
        userService.requireExistingUser(requesterEmail);
        return getOrCreateSettings();
    }

    public AutoCloseSettings updateSettings(String requesterEmail, AutoCloseSettingsRequest request) {
        User admin = userService.requireAdminByEmail(requesterEmail);
        AutoCloseSettings settings = getOrCreateSettings();

        if (request.getEnabled() != null) {
            settings.setEnabled(request.getEnabled());
        }
        if (request.getCutoffTime() != null) {
            settings.setCutoffTime(parseAndNormalizeTime(request.getCutoffTime()));
        }
        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            validateTimezone(request.getTimezone().trim());
            settings.setTimezone(request.getTimezone().trim());
        }
        settings.setUpdatedByEmail(admin.getEmail());

        AutoCloseSettings saved = repository.save(settings);
        auditLogService.logEvent(
            admin.getEmail(),
            "AUTO_CLOSE_SETTINGS_UPDATED",
            "Updated auto-close settings to enabled=" + saved.isEnabled()
                + ", cutoffTime=" + saved.getCutoffTime()
                + ", timezone=" + saved.getTimezone()
        );
        return saved;
    }

    public AutoCloseSettings getOrCreateSettings() {
        return repository.findById(AutoCloseSettings.SINGLETON_ID)
            .orElseGet(() -> {
                AutoCloseSettings settings = new AutoCloseSettings();
                settings.setId(AutoCloseSettings.SINGLETON_ID);
                settings.setEnabled(true);
                settings.setCutoffTime("23:59");
                settings.setTimezone(defaultTimezone);
                return repository.save(settings);
            });
    }

    public boolean shouldRunAutoCloseNow(AutoCloseSettings settings) {
        if (settings == null || !settings.isEnabled()) {
            return false;
        }

        ZoneId zoneId = ZoneId.of(settings.getTimezone());
        LocalDate today = LocalDate.now(zoneId);
        LocalTime now = LocalTime.now(zoneId).withSecond(0).withNano(0);
        LocalTime cutoff = LocalTime.parse(settings.getCutoffTime());

        return (settings.getLastRunDate() == null || !today.equals(settings.getLastRunDate()))
            && !now.isBefore(cutoff);
    }

    public void markRunCompleted(AutoCloseSettings settings) {
        if (settings == null) {
            return;
        }

        ZoneId zoneId = ZoneId.of(settings.getTimezone());
        settings.setLastRunDate(LocalDate.now(zoneId));
        settings.setUpdatedByEmail("SYSTEM");
        repository.save(settings);
    }

    public LocalTime getCutoffLocalTime(AutoCloseSettings settings) {
        return LocalTime.parse(settings.getCutoffTime());
    }

    private String parseAndNormalizeTime(String value) {
        LocalTime time = LocalTime.parse(value.trim());
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private void validateTimezone(String timezone) {
        ZoneId.of(timezone);
    }
}
