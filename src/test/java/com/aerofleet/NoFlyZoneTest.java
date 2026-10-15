package com.aerofleet;

import com.aerofleet.domain.model.GeoCoordinate;
import com.aerofleet.domain.model.NoFlyZone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NoFlyZoneTest {

    @Test
    public void testZoneViolation() {
        NoFlyZone zone = new NoFlyZone(1L, "Airport NFZ", 37.7850, -122.4050, 600.0, 0.0, 200.0, true);

        GeoCoordinate inside = new GeoCoordinate(37.7850, -122.4050, 50.0);
        assertTrue(zone.violatesZone(inside), "Center point must violate active zone");

        GeoCoordinate tooHigh = new GeoCoordinate(37.7850, -122.4050, 250.0);
        assertFalse(zone.violatesZone(tooHigh), "Point above ceiling should not violate zone");

        GeoCoordinate farAway = new GeoCoordinate(37.7000, -122.4050, 50.0);
        assertFalse(zone.violatesZone(farAway), "Distant point must not violate zone");
    }

    @Test
    public void testPathIntersection() {
        NoFlyZone zone = new NoFlyZone(1L, "Center NFZ", 37.7800, -122.4100, 500.0, 0.0, 200.0, true);

        // Path passing directly through center (North-South)
        GeoCoordinate north = new GeoCoordinate(37.7900, -122.4100, 80.0);
        GeoCoordinate south = new GeoCoordinate(37.7700, -122.4100, 80.0);

        assertTrue(zone.intersectsPath(north, south), "Path crossing through NFZ must be detected");

        // Clear parallel path far away to the west
        GeoCoordinate westP1 = new GeoCoordinate(37.7900, -122.4500, 80.0);
        GeoCoordinate westP2 = new GeoCoordinate(37.7700, -122.4500, 80.0);

        assertFalse(zone.intersectsPath(westP1, westP2), "Parallel path outside radius must not intersect");
    }
}
