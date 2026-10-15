package com.aerofleet.domain.model;

import com.aerofleet.domain.enums.DroneStatus;
import java.io.Serializable;

public class Waypoint implements Serializable {
    private GeoCoordinate coordinate;
    private double targetSpeedMps;
    private DroneStatus expectedStatus;
    private String actionDescription;

    public Waypoint() {
    }

    public Waypoint(GeoCoordinate coordinate, double targetSpeedMps, DroneStatus expectedStatus, String actionDescription) {
        this.coordinate = coordinate;
        this.targetSpeedMps = targetSpeedMps;
        this.expectedStatus = expectedStatus;
        this.actionDescription = actionDescription;
    }

    public GeoCoordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(GeoCoordinate coordinate) {
        this.coordinate = coordinate;
    }

    public double getTargetSpeedMps() {
        return targetSpeedMps;
    }

    public void setTargetSpeedMps(double targetSpeedMps) {
        this.targetSpeedMps = targetSpeedMps;
    }

    public DroneStatus getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(DroneStatus expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public String getActionDescription() {
        return actionDescription;
    }

    public void setActionDescription(String actionDescription) {
        this.actionDescription = actionDescription;
    }
}
