package com.aerofleet;

import com.aerofleet.domain.model.Location;
import com.aerofleet.domain.model.NoFlyZone;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NoFlyZoneTest {

    @Test
    public void testZoneViolation() {
        NoFlyZone zone = new NoFlyZone(1L, "Campus Helipad", 400.0, 300.0, 50.0, true);

        // Point inside radius 50: (410, 310) -> distance sqrt(10^2 + 10^2) = 14.14 <= 50
        Location inside = new Location(410.0, 310.0);
        assertTrue(zone.violatesZone(inside), "Point within radius must violate zone");

        // Point outside radius 50: (460, 300) -> distance 60 > 50
        Location outside = new Location(460.0, 300.0);
        assertFalse(zone.violatesZone(outside), "Point outside radius must not violate zone");
    }

    @Test
    public void testPathIntersection() {
        NoFlyZone zone = new NoFlyZone(1L, "Center Hazard", 400.0, 300.0, 50.0, true);

        // Path passing horizontally through (400, 300)
        Location west = new Location(300.0, 300.0);
        Location east = new Location(500.0, 300.0);
        assertTrue(zone.intersectsPath(west, east), "Path cutting through hazard zone must be detected");

        // Clear parallel path up north at Y = 200
        Location safeWest = new Location(300.0, 200.0);
        Location safeEast = new Location(500.0, 200.0);
        assertFalse(zone.intersectsPath(safeWest, safeEast), "Clear path outside radius must not intersect");
    }

    @Test
    public void testInactiveZoneDoesNotViolate() {
        NoFlyZone zone = new NoFlyZone(2L, "Temporary Construction Zone", 400.0, 300.0, 50.0, false);
        Location inside = new Location(410.0, 310.0);
        assertFalse(zone.violatesZone(inside), "Deactivated zone must not trigger violation");
        assertFalse(zone.intersectsPath(new Location(300.0, 300.0), new Location(500.0, 300.0)), "Deactivated zone must not block path");
    }
}
