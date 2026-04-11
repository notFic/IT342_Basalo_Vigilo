package edu.cit.basalo.vigilo.service;

import edu.cit.basalo.vigilo.entity.VisitorLog;
import edu.cit.basalo.vigilo.repository.VisitorLogRepository;
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

@Service
public class VisitorLogService {

    private static final String ACTIVE_STATUS = "Active";
    private static final String CHECKED_OUT_STATUS = "Checked-Out";

    private final VisitorLogRepository visitorLogRepository;

    public VisitorLogService(VisitorLogRepository visitorLogRepository) {
        this.visitorLogRepository = visitorLogRepository;
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
