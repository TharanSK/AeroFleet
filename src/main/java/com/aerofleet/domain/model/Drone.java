package com.aerofleet.domain.model;

import com.aerofleet.domain.enums.DroneModelType;
import com.aerofleet.domain.enums.DroneStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * Autonomous delivery drone entity navigating the 2D grid.
 */
@Entity
@Table(name = "drones")
public class Drone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String callsign;

    @Enumerated(EnumType.STRING)
    private DroneModelType modelType;

    private double x;
    private double y;
    private double speed;
    private double headingDegrees;
    private double batteryPercent;
    private double currentPayloadKg;

    @Enumerated(EnumType.STRING)
    private DroneStatus status;

    private Long currentDepotId;
    private Long assignedOrderId;

    private long totalFlightTimeSeconds;
    private double totalDistanceTraveled;
    private int successfulDeliveriesCount;

    @Transient
    private List<Waypoint> waypoints = new ArrayList<>();

    @Transient
    private int currentWaypointIndex = 0;

    public Drone() {
        this.status = DroneStatus.IDLE;
        this.batteryPercent = 100.0;
        this.speed = 0.0;
        this.headingDegrees = 0.0;
        this.currentPayloadKg = 0.0;
        this.totalFlightTimeSeconds = 0;
        this.totalDistanceTraveled = 0;
        this.successfulDeliveriesCount = 0;
    }

    public Drone(String callsign, DroneModelType modelType, double x, double y, Long depotId) {
        this();
        this.callsign = callsign;
        this.modelType = modelType;
        this.x = x;
        this.y = y;
        this.currentDepotId = depotId;
    }

    public Location getLocation() {
        return new Location(x, y);
    }

    public void setLocation(Location loc) {
        if (loc != null) {
            this.x = loc.getX();
            this.y = loc.getY();
        }
    }

    public Waypoint getCurrentWaypoint() {
        if (waypoints != null && currentWaypointIndex >= 0 && currentWaypointIndex < waypoints.size()) {
            return waypoints.get(currentWaypointIndex);
        }
        return null;
    }

    public boolean hasNextWaypoint() {
        return waypoints != null && currentWaypointIndex + 1 < waypoints.size();
    }

    public void advanceWaypoint() {
        this.currentWaypointIndex++;
    }

    public void clearFlightPlan() {
        if (this.waypoints != null) {
            this.waypoints.clear();
        }
        this.currentWaypointIndex = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public DroneModelType getModelType() {
        return modelType;
    }

    public void setModelType(DroneModelType modelType) {
        this.modelType = modelType;
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

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getHeadingDegrees() {
        return headingDegrees;
    }

    public void setHeadingDegrees(double headingDegrees) {
        this.headingDegrees = headingDegrees;
    }

    public double getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(double batteryPercent) {
        this.batteryPercent = Math.max(0.0, Math.min(100.0, batteryPercent));
    }

    public double getCurrentPayloadKg() {
        return currentPayloadKg;
    }

    public void setCurrentPayloadKg(double currentPayloadKg) {
        this.currentPayloadKg = currentPayloadKg;
    }

    public DroneStatus getStatus() {
        return status;
    }

    public void setStatus(DroneStatus status) {
        this.status = status;
    }

    public Long getCurrentDepotId() {
        return currentDepotId;
    }

    public void setCurrentDepotId(Long currentDepotId) {
        this.currentDepotId = currentDepotId;
    }

    public Long getAssignedOrderId() {
        return assignedOrderId;
    }

    public void setAssignedOrderId(Long assignedOrderId) {
        this.assignedOrderId = assignedOrderId;
    }

    public long getTotalFlightTimeSeconds() {
        return totalFlightTimeSeconds;
    }

    public void setTotalFlightTimeSeconds(long totalFlightTimeSeconds) {
        this.totalFlightTimeSeconds = totalFlightTimeSeconds;
    }

    public double getTotalDistanceTraveled() {
        return totalDistanceTraveled;
    }

    public void setTotalDistanceTraveled(double totalDistanceTraveled) {
        this.totalDistanceTraveled = totalDistanceTraveled;
    }

    public int getSuccessfulDeliveriesCount() {
        return successfulDeliveriesCount;
    }

    public void setSuccessfulDeliveriesCount(int successfulDeliveriesCount) {
        this.successfulDeliveriesCount = successfulDeliveriesCount;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints != null ? new ArrayList<>(waypoints) : new ArrayList<>();
        this.currentWaypointIndex = 0;
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    public void setCurrentWaypointIndex(int currentWaypointIndex) {
        this.currentWaypointIndex = currentWaypointIndex;
    }
}
