package com.aerofleet.simulation;

import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.model.Drone;
import com.aerofleet.service.WeatherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Educational battery drain & recharge model for delivery drones.
 * Demonstrates basic physics formulas: Base Drain + Payload Factor * Weather Drag.
 */
@Component
public class BatteryDrainModel {

    private final WeatherService weatherService;
    private final double chargingRatePercentPerSec;

    public BatteryDrainModel(
            WeatherService weatherService,
            @Value("${aerofleet.simulation.charging-rate-percent-per-sec:3.0}") double chargingRatePercentPerSec) {
        this.weatherService = weatherService;
        this.chargingRatePercentPerSec = chargingRatePercentPerSec;
    }

    /**
     * Calculates updated battery percentage after deltaSeconds.
     */
    public double updateBattery(Drone drone, double deltaSeconds) {
        if (drone == null || deltaSeconds <= 0.0) return 0.0;

        DroneStatus status = drone.getStatus();

        // 1. Recharging when docked at Depot
        if (status == DroneStatus.CHARGING) {
            double newCharge = drone.getBatteryPercent() + (chargingRatePercentPerSec * deltaSeconds);
            return Math.min(100.0, newCharge);
        }

        // 2. Stationary / Idle on pad
        if (status == DroneStatus.IDLE || status == DroneStatus.MAINTENANCE) {
            return drone.getBatteryPercent();
        }

        // 3. In-Flight Discharge: Base rate + Payload factor
        double baseDrainRatePerSec = 0.4; // 0.4% per second
        double maxPayload = drone.getModelType().getMaxPayloadKg();
        double payloadRatio = maxPayload > 0 ? (drone.getCurrentPayloadKg() / maxPayload) : 0.0;

        double payloadDrainRatePerSec = 0.3 * payloadRatio; // extra drain when carrying cargo
        double dragMultiplier = weatherService.getDragMultiplier();

        double totalDrainRate = (baseDrainRatePerSec + payloadDrainRatePerSec) * dragMultiplier;
        double drainAmount = totalDrainRate * deltaSeconds;

        double updatedPercent = drone.getBatteryPercent() - drainAmount;
        return Math.max(0.0, Math.min(100.0, updatedPercent));
    }

    public double getChargingRatePercentPerSec() {
        return chargingRatePercentPerSec;
    }
}
