package com.aerofleet.domain.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a 2D coordinate (x, y) on the simulation grid.
 * Demonstrates Core OOP: Encapsulation, Methods, and Euclidean Geometry.
 */
@Embeddable
public class Location implements Serializable {
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;

    public Location() {
        this(0.0, 0.0);
    }

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    /**
     * Calculates standard Euclidean (Pythagorean) distance to another location:
     * Distance = sqrt((x2 - x1)^2 + (y2 - y1)^2)
     */
    public double distanceTo(Location target) {
        if (target == null) return 0.0;
        double dx = this.x - target.x;
        double dy = this.y - target.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculates angle/bearing in degrees [0, 360) from this location to the target.
     */
    public double bearingTo(Location target) {
        if (target == null) return 0.0;
        double dx = target.x - this.x;
        double dy = target.y - this.y;
        double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
        return (angleDeg + 360.0) % 360.0;
    }

    /**
     * Calculates an intermediate coordinate by moving 'stepDistance' towards target.
     */
    public Location moveTowards(Location target, double stepDistance) {
        if (target == null || stepDistance <= 0.0) {
            return new Location(this.x, this.y);
        }

        double totalDist = distanceTo(target);
        if (totalDist <= stepDistance || totalDist == 0.0) {
            return new Location(target.x, target.y);
        }

        double fraction = stepDistance / totalDist;
        double newX = this.x + (target.x - this.x) * fraction;
        double newY = this.y + (target.y - this.y) * fraction;

        return new Location(newX, newY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location location)) return false;
        return Double.compare(location.x, x) == 0 && Double.compare(location.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("(%.1f, %.1f)", x, y);
    }
}
