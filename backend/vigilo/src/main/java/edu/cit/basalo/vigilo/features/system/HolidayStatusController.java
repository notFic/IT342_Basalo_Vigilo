package edu.cit.basalo.vigilo.features.system;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/system")
public class HolidayStatusController {

    private final HolidayStatusService holidayStatusService;

    public HolidayStatusController(HolidayStatusService holidayStatusService) {
        this.holidayStatusService = holidayStatusService;
    }

    @GetMapping("/holiday-status")
    public ResponseEntity<?> getHolidayStatus(Principal principal) {
        try {
            return ResponseEntity.ok(holidayStatusService.getHolidayStatus(principal.getName()));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        } catch (RuntimeException exception) {
            return ResponseEntity.internalServerError().body(exception.getMessage());
        }
    }
}
