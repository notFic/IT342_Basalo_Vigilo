package edu.cit.basalo.vigilo.features.notification;

import edu.cit.basalo.vigilo.features.user.User;
import edu.cit.basalo.vigilo.features.visitor.VisitorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class NotificationEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificationEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromEmail;
    private final String facilityManagerEmail;

    public NotificationEmailService(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${app.notifications.from-email:}") String fromEmail,
        @Value("${app.notifications.facility-manager-email:}") String facilityManagerEmail
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.facilityManagerEmail = facilityManagerEmail == null ? "" : facilityManagerEmail.trim();
    }

    public boolean sendWelcomeEmail(User user) {
        if (user == null || isBlank(user.getEmail())) {
            return false;
        }

        String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
        return sendEmail(
            user.getEmail(),
            "Welcome to Vigilo",
            "Hello " + (fullName.isBlank() ? "Staff Member" : fullName) + ",\n\n"
                + "Your Vigilo account has been created successfully.\n"
                + "Role: " + safe(user.getRole()) + "\n"
                + "Email: " + safe(user.getEmail()) + "\n\n"
                + "You may now sign in to the system.\n\n"
                + "Regards,\nVigilo System"
        );
    }

    public boolean sendVoidNotification(VisitorLog visitorLog, String adminEmail, String voidReason) {
        if (visitorLog == null) {
            return false;
        }

        Set<String> recipients = new LinkedHashSet<>();
        if (!isBlank(adminEmail)) {
            recipients.add(adminEmail.trim());
        }
        if (!isBlank(facilityManagerEmail)) {
            recipients.add(facilityManagerEmail.trim());
        }

        if (recipients.isEmpty()) {
            log.warn("Skipping void notification because no recipient email is configured.");
            return false;
        }

        String reasonLine = isBlank(voidReason) ? "Reason: Not provided" : "Reason: " + voidReason.trim();
        String subject = "Vigilo Record Voided: " + safe(visitorLog.getFullName());
        String body = "A visitor record was voided in Vigilo.\n\n"
            + "Visitor: " + safe(visitorLog.getFullName()) + "\n"
            + "Host: " + safe(visitorLog.getHostName()) + "\n"
            + "Destination: " + safe(visitorLog.getDestinationRoom()) + "\n"
            + "Current Status: " + safe(visitorLog.getStatus()) + "\n"
            + "Created By: " + safe(visitorLog.getCreatedByEmail()) + "\n"
            + "Voided By: " + safe(adminEmail) + "\n"
            + reasonLine + "\n"
            + "Time In: " + String.valueOf(visitorLog.getTimeIn()) + "\n"
            + "Time Out: " + String.valueOf(visitorLog.getTimeOut()) + "\n\n"
            + "This notification was generated automatically by Vigilo.";

        boolean sentAny = false;
        for (String recipient : recipients) {
            sentAny = sendEmail(recipient, subject, body) || sentAny;
        }
        return sentAny;
    }

    private boolean sendEmail(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Skipping email to {} because no JavaMailSender is configured.", to);
            return false;
        }

        if (isBlank(to)) {
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (!isBlank(fromEmail)) {
                message.setFrom(fromEmail);
            }
            message.setTo(to.trim());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
            return true;
        } catch (Exception exception) {
            log.error("Failed to send email to {}", to, exception);
            return false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
