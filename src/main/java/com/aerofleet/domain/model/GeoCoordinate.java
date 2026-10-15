package com.aerofleet.domain.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GeoCoordinate implements Serializable {
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private double latitude;
    private double longitude;
    private double altitudeMeters;

    public GeoCoordinate() {
    }

    public GeoCoordinate(double latitude, double longitude) {
        this(latitude, longitude, 0.0);
    }

    public GeoCoordinate(double latitude, double longitude, double altitudeMeters) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitudeMeters() {
        return altitudeMeters;
    }

    public void setAltitudeMeters(double altitudeMeters) {
        this.altitudeMeters = altitudeMeters;
    }

    /**
     * Calculates the great-circle surface distance in meters using Haversine formula.
     */
    public double distanceTo(GeoCoordinate target) {
        if (target == null) return 0.0;

        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(target.latitude);
        double deltaLat = Math.toRadians(target.latitude - this.latitude);
        double deltaLon = Math.toRadians(target.longitude - this.longitude);

        double a = Math.sin(deltaLat / 2.0) * Math.sin(deltaLat / 2.0) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2.0) * Math.sin(deltaLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Calculates initial azimuth bearing in degrees [0, 360) from this coordinate to target.
     */
    public double bearingTo(GeoCoordinate target) {
        if (target == null) return 0.0;

        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(target.latitude);
        double deltaLon = Math.toRadians(target.longitude - this.longitude);

        double y = Math.sin(deltaLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }

    /**
     * Calculates a new coordinate by moving a specific distance (meters) along a bearing (degrees).
     */
    public GeoCoordinate moveTowards(GeoCoordinate target, double distanceMeters) {
        if (target == null || distanceMeters <= 0.0) {
            return new GeoCoordinate(this.latitude, this.longitude, this.altitudeMeters);
        }

        double totalDist = distanceTo(target);
        if (totalDist <= distanceMeters || totalDist == 0.0) {
            return new GeoCoordinate(target.latitude, target.longitude, target.altitudeMeters);
        }

        double fraction = distanceMeters / totalDist;
        double newLat = this.latitude + (target.latitude - this.latitude) * fraction;
        double newLon = this.longitude + (target.longitude - this.longitude) * fraction;
        double newAlt = this.altitudeMeters + (target.altitudeMeters - this.altitudeMeters) * fraction;

        return new GeoCoordinate(newLat, newLon, newAlt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeoCoordinate that)) return false;
        return Double.compare(that.latitude, latitude) == 0 &&
               Double.compare(that.longitude, longitude) == 0 &&
               Double.compare(that.altitudeMeters, altitudeMeters) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, altitudeMeters);
    }

    @Override
    public String toString() {
        return "GeoCoordinate{" +
                "lat=" + latitude +
                ", lon=" + longitude +
                ", alt=" + altitudeMeters + "m" +
                '}';
    }
}
