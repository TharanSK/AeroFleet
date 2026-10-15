package com.aerofleet;

import com.aerofleet.domain.model.Location;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class KinematicsTest {

    @Test
    public void testEuclideanPythagoreanDistance() {
        // Standard 3-4-5 right triangle: (0,0) to (30, 40) is 50.0
        Location origin = new Location(0.0, 0.0);
        Location target = new Location(30.0, 40.0);

        double distance = origin.distanceTo(target);
        assertEquals(50.0, distance, 0.001, "Distance in 3-4-5 triangle must equal 50.0");
    }

    @Test
    public void testBearingCalculation() {
        Location origin = new Location(100.0, 100.0);
        Location east = new Location(200.0, 100.0);
        Location south = new Location(100.0, 200.0);

        double bearingEast = origin.bearingTo(east);
        assertEquals(0.0, bearingEast, 0.5, "Bearing directly east/right must be 0 degrees");

        double bearingSouth = origin.bearingTo(south);
        assertEquals(90.0, bearingSouth, 0.5, "Bearing directly south/down must be 90 degrees");
    }

    @Test
    public void testMoveTowards() {
        Location start = new Location(0.0, 0.0);
        Location target = new Location(100.0, 0.0);
        double step = 25.0;

        Location moved = start.moveTowards(target, step);

        assertEquals(25.0, moved.getX(), 0.001, "X should advance by exactly step distance");
        assertEquals(0.0, moved.getY(), 0.001, "Y should stay constant");
        assertEquals(25.0, start.distanceTo(moved), 0.001);
    }
}
