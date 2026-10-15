package com.aerofleet.dto;

import com.aerofleet.domain.enums.WeatherCondition;
import java.util.ArrayList;
import java.util.List;

public class FleetSnapshotDTO {
    private long timestamp;
    private boolean simulationRunning;
    private double speedMultiplier;
    private long simTimeElapsedSeconds;
    private WeatherCondition weatherCondition;
    private double windSpeedMps;
    private double windDirectionDeg;

    private int totalDrones;
    private int activeDeliveries;
    private int chargingDrones;
    private int idleDrones;
    private int pendingOrders;
    private int completedOrders;

    private List<TelemetryPacketDTO> drones = new ArrayList<>();

    public FleetSnapshotDTO() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    public void setSimulationRunning(boolean simulationRunning) {
        this.simulationRunning = simulationRunning;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public long getSimTimeElapsedSeconds() {
        return simTimeElapsedSeconds;
    }

    public void setSimTimeElapsedSeconds(long simTimeElapsedSeconds) {
        this.simTimeElapsedSeconds = simTimeElapsedSeconds;
    }

    public WeatherCondition getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(WeatherCondition weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public double getWindSpeedMps() {
        return windSpeedMps;
    }

    public void setWindSpeedMps(double windSpeedMps) {
        this.windSpeedMps = windSpeedMps;
    }

    public double getWindDirectionDeg() {
        return windDirectionDeg;
    }

    public void setWindDirectionDeg(double windDirectionDeg) {
        this.windDirectionDeg = windDirectionDeg;
    }

    public int getTotalDrones() {
        return totalDrones;
    }

    public void setTotalDrones(int totalDrones) {
        this.totalDrones = totalDrones;
    }

    public int getActiveDeliveries() {
        return activeDeliveries;
    }

    public void setActiveDeliveries(int activeDeliveries) {
        this.activeDeliveries = activeDeliveries;
    }

    public int getChargingDrones() {
        return chargingDrones;
    }

    public void setChargingDrones(int chargingDrones) {
        this.chargingDrones = chargingDrones;
    }

    public int getIdleDrones() {
        return idleDrones;
    }

    public void setIdleDrones(int idleDrones) {
        this.idleDrones = idleDrones;
    }

    public int getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(int pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public int getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(int completedOrders) {
        this.completedOrders = completedOrders;
    }

    public List<TelemetryPacketDTO> getDrones() {
        return drones;
    }

    public void setDrones(List<TelemetryPacketDTO> drones) {
        this.drones = drones != null ? drones : new ArrayList<>();
    }
}
