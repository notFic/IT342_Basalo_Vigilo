package edu.cit.basalo.vigilo.features.location;
import edu.cit.basalo.vigilo.features.location.Location;
import edu.cit.basalo.vigilo.features.location.LocationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LocationService {
    private final LocationRepository locationRepository;
    public LocationService(LocationRepository repo) { this.locationRepository = repo; }
    public List<Location> getAllLocations() { return locationRepository.findAll(); }
    public Location addLocation(Location loc) { return locationRepository.save(loc); }
}
