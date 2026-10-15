package com.aerofleet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Restricted Airspace / Hazard Zone on the 2D grid.
 */
@Entity
@Table(name = "no_fly_zones")
public class NoFlyZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "center_x")
    private double centerX;

    @Column(name = "center_y")
    private double centerY;

    private double radius;
    private boolean active;

    public NoFlyZone() {
    }

    public NoFlyZone(Long id, String name, double centerX, double centerY, double radius, boolean active) {
        this.id = id;
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.active = active;
    }

    public Location getCenterLocation() {
        return new Location(centerX, centerY);
    }

    /**
     * Checks if a 2D coordinate falls inside this circular zone:
     * (x - cx)^2 + (y - cy)^2 <= radius^2
     */
    public boolean violatesZone(Location loc) {
        if (!active || loc == null) return false;
        double dist = getCenterLocation().distanceTo(loc);
        return dist <= radius;
    }

    /**
     * Checks if a line segment between p1 and p2 passes inside this circular zone.
     */
    public boolean intersectsPath(Location p1, Location p2) {
        if (!active || p1 == null || p2 == null) return false;

        Location center = getCenterLocation();
        double d1 = p1.distanceTo(center);
        double d2 = p2.distanceTo(center);

        if (d1 <= radius || d2 <= radius) {
            return true;
        }

        // Sample points along line segment
        int steps = 12;
        for (int i = 1; i < steps; i++) {
            double fraction = (double) i / steps;
            double sampleX = p1.getX() + (p2.getX() - p1.getX()) * fraction;
            double sampleY = p1.getY() + (p2.getY() - p1.getY()) * fraction;
            if (violatesZone(new Location(sampleX, sampleY))) {
                return true;
            }
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
