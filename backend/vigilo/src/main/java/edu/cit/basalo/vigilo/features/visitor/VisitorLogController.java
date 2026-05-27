package edu.cit.basalo.vigilo.features.visitor;

import java.security.Principal;

import edu.cit.basalo.vigilo.features.visitor.VisitorLog;
import edu.cit.basalo.vigilo.features.visitor.VisitorLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import edu.cit.basalo.vigilo.features.user.UserService;

@RestController
@RequestMapping("/api/v1/logs")
public class VisitorLogController {

    private final VisitorLogService visitorLogService;
    private final UserService userService;

    public VisitorLogController(VisitorLogService visitorLogService, UserService userService) {
        this.visitorLogService = visitorLogService;
        this.userService = userService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkInVisitor(
        Principal principal,
        @RequestParam String fullName,
        @RequestParam String contactNumber,
        @RequestParam String hostName,
        @RequestParam String visitorType,
        @RequestParam String destinationRoom,
        @RequestParam String purpose,
        @RequestParam(defaultValue = "false") boolean extendedVisit,
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
                principal.getName(),
                idImage
            ));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body("Failed to store the visitor ID attachment.");
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveLogs(Principal principal) {
        try {
            return ResponseEntity.ok(visitorLogService.getActiveLogs(principal.getName()));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        }
    }

    @PutMapping("/{logId}/check-out")
    public ResponseEntity<?> checkOutVisitor(
        Principal principal,
        @PathVariable Long logId
    ) {
        try {
            return ResponseEntity.ok(visitorLogService.checkOutVisitor(logId, principal.getName()));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistoricalLogs(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query
    ) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        try {
            return ResponseEntity.ok(visitorLogService.getHistoricalLogs(pageable, principal.getName(), query));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        }
    }

    @PutMapping("/{logId}/void")
    public ResponseEntity<?> voidVisitorLog(
        Principal principal,
        @PathVariable Long logId,
        @RequestParam(required = false) String reason
    ) {
        try {
            return ResponseEntity.ok(visitorLogService.voidVisitorLog(logId, principal.getName(), reason));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    @PostMapping("/auto-close/run")
    public ResponseEntity<?> runAutoCloseJob(Principal principal) {
        try {
            userService.requireAdminByEmail(principal.getName());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "closedCount", visitorLogService.runAutoCloseJob(),
                "message", "Auto-close job executed successfully."
            ));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
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
