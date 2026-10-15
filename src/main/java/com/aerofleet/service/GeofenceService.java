package com.aerofleet.service;

import com.aerofleet.domain.model.Location;
import com.aerofleet.domain.model.NoFlyZone;
import com.aerofleet.repository.NoFlyZoneRepository;
import org.springframework.stereotype.Service;
import java.util.Comparator;
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
        return getClosestIntersectedZone(start, end);
    }

    public Optional<NoFlyZone> getClosestIntersectedZone(Location start, Location end) {
        if (start == null || end == null) return Optional.empty();
        return getActiveZones().stream()
                .filter(zone -> zone.intersectsPath(start, end))
                .min(Comparator.comparingDouble(zone -> zone.getCenterLocation().distanceTo(start)));
    }

    /**
     * Calculates an optimal 2D detour waypoint routing around the circular No-Fly Zone.
     * Evaluates both perpendicular normal directions and selects the shorter perimeter path.
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

        // Perpendicular normal unit vector: (-dy, dx)
        double nx = -dy / length;
        double ny = dx / length;

        // Candidate 1 (+normal) and Candidate 2 (-normal)
        Location bypass1 = new Location(
                Math.max(20.0, Math.min(780.0, center.getX() + (nx * safetyDistance))),
                Math.max(20.0, Math.min(580.0, center.getY() + (ny * safetyDistance)))
        );
        Location bypass2 = new Location(
                Math.max(20.0, Math.min(780.0, center.getX() - (nx * safetyDistance))),
                Math.max(20.0, Math.min(580.0, center.getY() - (ny * safetyDistance)))
        );

        // Avoid pushing into another restricted zone
        boolean v1 = isPointInRestrictedAirspace(bypass1);
        boolean v2 = isPointInRestrictedAirspace(bypass2);

        if (!v1 && v2) return bypass1;
        if (v1 && !v2) return bypass2;

        // Choose shorter total flight detour distance
        double dist1 = start.distanceTo(bypass1) + bypass1.distanceTo(end);
        double dist2 = start.distanceTo(bypass2) + bypass2.distanceTo(end);

        return dist1 <= dist2 ? bypass1 : bypass2;
    }
}
