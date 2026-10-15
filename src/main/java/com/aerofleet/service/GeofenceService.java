package com.aerofleet.service;

import com.aerofleet.domain.model.Location;
import com.aerofleet.domain.model.NoFlyZone;
import com.aerofleet.repository.NoFlyZoneRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Service managing 2D restricted airspace and obstacle avoidance.
 */
@Service
public class GeofenceService {

    private final NoFlyZoneRepository noFlyZoneRepository;

    public GeofenceService(NoFlyZoneRepository noFlyZoneRepository) {
        this.noFlyZoneRepository = noFlyZoneRepository;
    }

    public List<NoFlyZone> getActiveZones() {
        return noFlyZoneRepository.findByActiveTrue();
    }

    public boolean isPointInRestrictedAirspace(Location loc) {
        if (loc == null) return false;
        return getActiveZones().stream().anyMatch(zone -> zone.violatesZone(loc));
    }

    public Optional<NoFlyZone> getIntersectedZone(Location start, Location end) {
        if (start == null || end == null) return Optional.empty();
        return getActiveZones().stream()
                .filter(zone -> zone.intersectsPath(start, end))
                .findFirst();
    }

    /**
     * Calculates a simple 2D detour waypoint routing around the circular No-Fly Zone.
     */
    public Location calculateBypassWaypoint(Location start, Location end, NoFlyZone zone) {
        Location center = zone.getCenterLocation();
        double safetyDistance = zone.getRadius() * 1.35;

        // Vector from start to end
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0.0) {
            return new Location(center.getX() + safetyDistance, center.getY());
        }

        // Perpendicular normal vector: (-dy, dx)
        double nx = -dy / length;
        double ny = dx / length;

        // Push detour point outward from circle center along normal
        double bypassX = center.getX() + (nx * safetyDistance);
        double bypassY = center.getY() + (ny * safetyDistance);

        // Keep inside reasonable grid bounds [10, 790]
        bypassX = Math.max(20.0, Math.min(780.0, bypassX));
        bypassY = Math.max(20.0, Math.min(580.0, bypassY));

        return new Location(bypassX, bypassY);
    }
}
