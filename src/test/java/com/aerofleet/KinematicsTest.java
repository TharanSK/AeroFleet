package com.aerofleet;

import com.aerofleet.domain.model.GeoCoordinate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KinematicsTest {

    @Test
    public void testHaversineDistance() {
        // San Francisco (37.7749, -122.4194) to Oakland (37.8044, -122.2712) ~13.4 km
        GeoCoordinate sf = new GeoCoordinate(37.7749, -122.4194, 0.0);
        GeoCoordinate oakland = new GeoCoordinate(37.8044, -122.2712, 0.0);

        double distance = sf.distanceTo(oakland);
        assertTrue(distance > 13000.0 && distance < 14000.0, "Distance should be ~13.4km, got: " + distance);
    }

    @Test
    public void testBearingCalculation() {
        GeoCoordinate origin = new GeoCoordinate(0.0, 0.0, 0.0);
        GeoCoordinate north = new GeoCoordinate(1.0, 0.0, 0.0);
        GeoCoordinate east = new GeoCoordinate(0.0, 1.0, 0.0);

        double bearingNorth = origin.bearingTo(north);
        assertEquals(0.0, bearingNorth, 1.0, "Bearing towards north should be ~0 degrees");

        double bearingEast = origin.bearingTo(east);
        assertEquals(90.0, bearingEast, 1.0, "Bearing towards east should be ~90 degrees");
    }

    @Test
    public void testMoveTowards() {
        GeoCoordinate p1 = new GeoCoordinate(37.7749, -122.4194, 0.0);
        GeoCoordinate p2 = new GeoCoordinate(37.7849, -122.4194, 100.0);

        double totalDist = p1.distanceTo(p2);
        double step = 200.0;

        GeoCoordinate intermediate = p1.moveTowards(p2, step);

        double distFromStart = p1.distanceTo(intermediate);
        assertEquals(step, distFromStart, 2.0, "Intermediate point should be exactly step distance away");
        assertTrue(intermediate.getAltitudeMeters() > 0.0, "Altitude should interpolate upward");
    }
}
