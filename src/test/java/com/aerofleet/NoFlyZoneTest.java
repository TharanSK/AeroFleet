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

    @Test
    public void testExactLineIntersectionSmallRadius() {
        // Small hazard zone at center (400, 300) with small radius of 25 units
        NoFlyZone smallZone = new NoFlyZone(3L, "Small Mast", 400.0, 300.0, 25.0, true);

        // Long diagonal flight from (150, 180) to (650, 420) that passes exactly through (400, 300)
        Location start = new Location(150.0, 180.0);
        Location end = new Location(650.0, 420.0);

        assertTrue(smallZone.intersectsPath(start, end),
                "Exact projection must detect diagonal intersection even when radius is small");

        // Clear parallel path shifted by 40 units north (Y - 40)
        Location safeStart = new Location(150.0, 140.0);
        Location safeEnd = new Location(650.0, 380.0);
        assertFalse(smallZone.intersectsPath(safeStart, safeEnd),
                "Path passing outside radius must not intersect");
    }
}
