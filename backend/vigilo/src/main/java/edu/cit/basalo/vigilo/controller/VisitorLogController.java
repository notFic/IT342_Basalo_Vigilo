package edu.cit.basalo.vigilo.controller;

import edu.cit.basalo.vigilo.entity.VisitorLog;
import edu.cit.basalo.vigilo.service.VisitorLogService;
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
}
