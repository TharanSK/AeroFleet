package com.aerofleet.service;

import com.aerofleet.domain.model.GeoCoordinate;
import com.aerofleet.domain.model.NoFlyZone;
import com.aerofleet.repository.NoFlyZoneRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class GeofenceService {

    private final NoFlyZoneRepository noFlyZoneRepository;

    public GeofenceService(NoFlyZoneRepository noFlyZoneRepository) {
        this.noFlyZoneRepository = noFlyZoneRepository;
    }

    public List<NoFlyZone> getActiveZones() {
        return noFlyZoneRepository.findByActiveTrue();
    }

    public boolean isPointInRestrictedAirspace(GeoCoordinate coord) {
        if (coord == null) return false;
        return getActiveZones().stream().anyMatch(zone -> zone.violatesZone(coord));
    }

    public Optional<NoFlyZone> getIntersectedZone(GeoCoordinate start, GeoCoordinate end) {
        if (start == null || end == null) return Optional.empty();
        return getActiveZones().stream()
                .filter(zone -> zone.intersectsPath(start, end))
                .findFirst();
    }

    /**
     * Calculates an avoidance waypoint offset perpendicular to the line between start and end,
     * routing around the violated No-Fly Zone with a safety buffer.
     */
    public GeoCoordinate calculateBypassWaypoint(GeoCoordinate start, GeoCoordinate end, NoFlyZone zone) {
        GeoCoordinate center = zone.getCenterCoordinate();
        double safetyRadiusMeters = zone.getRadiusMeters() * 1.30; // 30% margin

        // Bearing from start to center of zone
        double bearingToCenter = start.bearingTo(center);

        // Deflect 75 degrees away from center
        double detourBearing = (bearingToCenter + 75.0) % 360.0;

        // Create bypass point outside safety perimeter
        GeoCoordinate bypass = center.moveTowards(
                new GeoCoordinate(
                        center.getLatitude() + 0.05 * Math.cos(Math.toRadians(detourBearing)),
                        center.getLongitude() + 0.05 * Math.sin(Math.toRadians(detourBearing)),
                        Math.max(start.getAltitudeMeters(), zone.getMaxAltitudeMeters() + 10.0)
                ),
                safetyRadiusMeters
        );

        bypass.setAltitudeMeters(start.getAltitudeMeters());
        return bypass;
    }
}
