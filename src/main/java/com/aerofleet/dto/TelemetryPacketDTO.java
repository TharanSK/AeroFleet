package com.aerofleet.dto;

import com.aerofleet.domain.enums.DroneModelType;
import com.aerofleet.domain.enums.DroneStatus;

public class TelemetryPacketDTO {
    private Long droneId;
    private String callsign;
    private DroneModelType modelType;
    private double latitude;
    private double longitude;
    private double altitudeMeters;
    private double speedMps;
    private double headingDegrees;
    private double batteryPercent;
    private double payloadKg;
    private DroneStatus status;
    private Long assignedOrderId;
    private Double targetLat;
    private Double targetLon;
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

    public double getSpeedMps() {
        return speedMps;
    }

    public void setSpeedMps(double speedMps) {
        this.speedMps = speedMps;
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

    public Double getTargetLat() {
        return targetLat;
    }

    public void setTargetLat(Double targetLat) {
        this.targetLat = targetLat;
    }

    public Double getTargetLon() {
        return targetLon;
    }

    public void setTargetLon(Double targetLon) {
        this.targetLon = targetLon;
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
