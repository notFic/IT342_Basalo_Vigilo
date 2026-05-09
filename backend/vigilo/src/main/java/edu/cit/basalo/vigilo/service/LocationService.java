package edu.cit.basalo.vigilo.service;
import edu.cit.basalo.vigilo.entity.Location;
import edu.cit.basalo.vigilo.repository.LocationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LocationService {
    private final LocationRepository locationRepository;
    public LocationService(LocationRepository repo) { this.locationRepository = repo; }
    public List<Location> getAllLocations() { return locationRepository.findAll(); }
    public Location addLocation(Location loc) { return locationRepository.save(loc); }
}
