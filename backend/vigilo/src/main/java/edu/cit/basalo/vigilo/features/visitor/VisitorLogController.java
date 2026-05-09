package edu.cit.basalo.vigilo.features.visitor;

import edu.cit.basalo.vigilo.features.visitor.VisitorLog;
import edu.cit.basalo.vigilo.features.visitor.VisitorLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
public class VisitorLogController {

    private final VisitorLogService visitorLogService;

    public VisitorLogController(VisitorLogService visitorLogService) {
        this.visitorLogService = visitorLogService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkInVisitor(
        @RequestParam String fullName,
        @RequestParam String contactNumber,
        @RequestParam String hostName,
        @RequestParam String visitorType,
        @RequestParam String destinationRoom,
        @RequestParam String purpose,
        @RequestParam(defaultValue = "false") boolean extendedVisit,
        @RequestParam String createdByEmail,
        @RequestParam("idImage") MultipartFile idImage
    ) {
        try {
            return ResponseEntity.ok(visitorLogService.createVisitorLog(
                fullName,
                contactNumber,
                hostName,
                visitorType,
                destinationRoom,
                purpose,
                extendedVisit,
                createdByEmail,
                idImage
            ));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body("Failed to store the visitor ID attachment.");
        }
    }

    @GetMapping("/active")
    public List<VisitorLog> getActiveLogs() {
        return visitorLogService.getActiveLogs();
    }

    @PutMapping("/{logId}/check-out")
    public ResponseEntity<?> checkOutVisitor(@PathVariable Long logId, @RequestParam String updatedByEmail) {
        try {
            return ResponseEntity.ok(visitorLogService.checkOutVisitor(logId, updatedByEmail));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistoricalLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ResponseEntity.ok(visitorLogService.getHistoricalLogs(pageable));
    }

    @PutMapping("/{logId}/void")
    public ResponseEntity<?> voidVisitorLog(@PathVariable Long logId, @RequestParam String updatedByEmail) {
        try {
            return ResponseEntity.ok(visitorLogService.voidVisitorLog(logId, updatedByEmail));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping("/images/**")
    public ResponseEntity<org.springframework.core.io.Resource> getImage(jakarta.servlet.http.HttpServletRequest request) {
        try {
            String fullPath = request.getRequestURI();
            String filePath = fullPath.split("/images/")[1];
            java.nio.file.Path path = java.nio.file.Paths.get(filePath).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                String contentType = java.nio.file.Files.probeContentType(path);
                if (contentType == null) contentType = "application/octet-stream";
                return ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
