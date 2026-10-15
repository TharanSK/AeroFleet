package com.aerofleet.dto;

import com.aerofleet.domain.enums.DroneModelType;
import com.aerofleet.domain.enums.DroneStatus;

/**
 * Real-time telemetry snapshot packet for a single drone.
 */
public class TelemetryPacketDTO {
    private Long droneId;
    private String callsign;
    private DroneModelType modelType;
    private double x;
    private double y;
    private double speed;
    private double headingDegrees;
    private double batteryPercent;
    private double payloadKg;
    private DroneStatus status;
    private Long assignedOrderId;
    private Double targetX;
    private Double targetY;
    private Long depotId;
    private int waypointIndex;
    private int totalWaypoints;

    public TelemetryPacketDTO() {
    }

    public Long getDroneId() {
        return droneId;
    }

    public void setDroneId(Long droneId) {
        this.droneId = droneId;
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
        this.batteryPercent = batteryPercent;
    }

    public double getPayloadKg() {
        return payloadKg;
    }

    public void setPayloadKg(double payloadKg) {
        this.payloadKg = payloadKg;
    }

    public DroneStatus getStatus() {
        return status;
    }

    public void setStatus(DroneStatus status) {
        this.status = status;
    }

    public Long getAssignedOrderId() {
        return assignedOrderId;
    }

    public void setAssignedOrderId(Long assignedOrderId) {
        this.assignedOrderId = assignedOrderId;
    }

    public Double getTargetX() {
        return targetX;
    }

    public void setTargetX(Double targetX) {
        this.targetX = targetX;
    }

    public Double getTargetY() {
        return targetY;
    }

    public void setTargetY(Double targetY) {
        this.targetY = targetY;
    }

    public Long getDepotId() {
        return depotId;
    }

    public void setDepotId(Long depotId) {
        this.depotId = depotId;
    }

    public int getWaypointIndex() {
        return waypointIndex;
    }

    public void setWaypointIndex(int waypointIndex) {
        this.waypointIndex = waypointIndex;
    }

    public int getTotalWaypoints() {
        return totalWaypoints;
    }

    public void setTotalWaypoints(int totalWaypoints) {
        this.totalWaypoints = totalWaypoints;
    }
}
