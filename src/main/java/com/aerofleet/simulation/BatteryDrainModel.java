package com.aerofleet.simulation;

import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.model.Drone;
import com.aerofleet.service.WeatherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
     * Computes the new battery percentage for a drone after deltaSeconds.
     */
    public double updateBattery(Drone drone, double deltaSeconds, double altitudeChangeMeters) {
        if (drone == null || deltaSeconds <= 0.0) return 0.0;

        DroneStatus status = drone.getStatus();

        // If charging at depot pad
        if (status == DroneStatus.CHARGING) {
            double newCharge = drone.getBatteryPercent() + (chargingRatePercentPerSec * deltaSeconds);
            return Math.min(100.0, newCharge);
        }

        // If stationary and idle on the ground, negligible drain
        if (status == DroneStatus.IDLE || status == DroneStatus.MAINTENANCE) {
            return drone.getBatteryPercent();
        }

        double capacityWh = drone.getModelType().getBatteryCapacityWh();
        double maxPayload = drone.getModelType().getMaxPayloadKg();
        double currentPayload = Math.min(maxPayload, drone.getCurrentPayloadKg());
        double dragMultiplier = weatherService.getDragMultiplier();

        // Nominal power draw: 220W base + payload scaling
        double payloadRatio = maxPayload > 0 ? (currentPayload / maxPayload) : 0.0;
        double basePowerWatts = 200.0 + (180.0 * payloadRatio);

        // Climb power penalty or descent power reduction
        if (altitudeChangeMeters > 0.05) {
            basePowerWatts += 120.0; // Extra thrust to gain altitude
        } else if (altitudeChangeMeters < -0.05) {
            basePowerWatts *= 0.65;  // Low throttle descent
        }

        // Apply environmental drag
        double totalPowerWatts = basePowerWatts * dragMultiplier;

        // Energy consumed in watt-hours during deltaSeconds
        double energyConsumedWh = (totalPowerWatts * deltaSeconds) / 3600.0;

        // Convert Wh to battery percentage
        double percentDrained = (energyConsumedWh / capacityWh) * 100.0;

        double updatedPercent = drone.getBatteryPercent() - percentDrained;
        return Math.max(0.0, Math.min(100.0, updatedPercent));
    }

    public double getChargingRatePercentPerSec() {
        return chargingRatePercentPerSec;
    }
}
