package edu.cit.basalo.vigilo.features.location;
import edu.cit.basalo.vigilo.features.location.Location;
import edu.cit.basalo.vigilo.features.location.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {
    private final LocationService locationService;
    public LocationController(LocationService locationService) { this.locationService = locationService; }
    @GetMapping
    public ResponseEntity<?> getAll() { return ResponseEntity.ok(locationService.getAllLocations()); }
    @PostMapping
    public ResponseEntity<?> addLocation(@RequestBody Location location) { return ResponseEntity.ok(locationService.addLocation(location)); }
}
