package com.aerofleet.domain.model;

import com.aerofleet.domain.enums.DroneStatus;
import java.io.Serializable;

/**
 * Represents a navigational waypoint in a flight plan.
 */
public class Waypoint implements Serializable {
    private Location location;
    private double targetSpeed;
    private DroneStatus expectedStatus;
    private String actionDescription;

    public Waypoint() {
    }

    public Waypoint(Location location, double targetSpeed, DroneStatus expectedStatus, String actionDescription) {
        this.location = location;
        this.targetSpeed = targetSpeed;
        this.expectedStatus = expectedStatus;
        this.actionDescription = actionDescription;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getTargetSpeed() {
        return targetSpeed;
    }

    public void setTargetSpeed(double targetSpeed) {
        this.targetSpeed = targetSpeed;
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
