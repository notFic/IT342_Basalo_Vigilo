package edu.cit.basalo.vigilo.features.location;
import edu.cit.basalo.vigilo.features.location.Location;
import edu.cit.basalo.vigilo.features.location.LocationService;
import edu.cit.basalo.vigilo.features.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {
    private final LocationService locationService;
    private final UserService userService;
    public LocationController(LocationService locationService, UserService userService) {
        this.locationService = locationService;
        this.userService = userService;
    }
    @GetMapping
    public ResponseEntity<?> getAll(Principal principal) {
        try {
            userService.requireExistingUser(principal.getName());
            return ResponseEntity.ok(locationService.getAllLocations());
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        }
    }
    @PostMapping
    public ResponseEntity<?> addLocation(
        Principal principal,
        @RequestBody Location location
    ) {
        try {
            userService.requireAdminByEmail(principal.getName());
            return ResponseEntity.ok(locationService.addLocation(location));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).body(exception.getReason());
        }
    }
}
