package com.aerofleet.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "no_fly_zones")
public class NoFlyZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double centerLatitude;
    private double centerLongitude;
    private double radiusMeters;
    private double minAltitudeMeters;
    private double maxAltitudeMeters;
    private boolean active;

    public NoFlyZone() {
    }

    public NoFlyZone(Long id, String name, double centerLatitude, double centerLongitude, double radiusMeters, double minAltitudeMeters, double maxAltitudeMeters, boolean active) {
        this.id = id;
        this.name = name;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.radiusMeters = radiusMeters;
        this.minAltitudeMeters = minAltitudeMeters;
        this.maxAltitudeMeters = maxAltitudeMeters;
        this.active = active;
    }

    public GeoCoordinate getCenterCoordinate() {
        return new GeoCoordinate(centerLatitude, centerLongitude, (minAltitudeMeters + maxAltitudeMeters) / 2.0);
    }

    public boolean violatesZone(GeoCoordinate coord) {
        if (!active || coord == null) return false;

        // Check vertical profile
        if (coord.getAltitudeMeters() < minAltitudeMeters || coord.getAltitudeMeters() > maxAltitudeMeters) {
            return false;
        }

        // Check horizontal radial distance
        double distance = getCenterCoordinate().distanceTo(coord);
        return distance <= radiusMeters;
    }

    /**
     * Checks if line segment between p1 and p2 passes closer to center than radius.
     */
    public boolean intersectsPath(GeoCoordinate p1, GeoCoordinate p2) {
        if (!active || p1 == null || p2 == null) return false;

        GeoCoordinate center = getCenterCoordinate();
        double d1 = p1.distanceTo(center);
        double d2 = p2.distanceTo(center);

        // If either endpoint is inside the zone
        if (d1 <= radiusMeters || d2 <= radiusMeters) {
            return true;
        }

        // Sampling points along the segment for robust circular boundary check
        int steps = 15;
        for (int i = 1; i < steps; i++) {
            double fraction = (double) i / steps;
            double sampleLat = p1.getLatitude() + (p2.getLatitude() - p1.getLatitude()) * fraction;
            double sampleLon = p1.getLongitude() + (p2.getLongitude() - p1.getLongitude()) * fraction;
            double sampleAlt = p1.getAltitudeMeters() + (p2.getAltitudeMeters() - p1.getAltitudeMeters()) * fraction;
            GeoCoordinate samplePoint = new GeoCoordinate(sampleLat, sampleLon, sampleAlt);

            if (violatesZone(samplePoint)) {
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

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public double getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public double getMinAltitudeMeters() {
        return minAltitudeMeters;
    }

    public void setMinAltitudeMeters(double minAltitudeMeters) {
        this.minAltitudeMeters = minAltitudeMeters;
    }

    public double getMaxAltitudeMeters() {
        return maxAltitudeMeters;
    }

    public void setMaxAltitudeMeters(double maxAltitudeMeters) {
        this.maxAltitudeMeters = maxAltitudeMeters;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
