package com.aerofleet;

import com.aerofleet.domain.enums.DroneModelType;
import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.model.Drone;
import com.aerofleet.service.WeatherService;
import com.aerofleet.simulation.BatteryDrainModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BatteryDrainModelTest {

    private BatteryDrainModel batteryDrainModel;
    private WeatherService weatherService;

    @BeforeEach
    public void setup() {
        weatherService = new WeatherService("CLEAR", 4.0, 45.0);
        batteryDrainModel = new BatteryDrainModel(weatherService, 3.0);
    }

    @Test
    public void testBatteryDischargeUnderPayload() {
        Drone drone = new Drone("TEST-01", DroneModelType.QUADCOPTER_STANDARD, 37.77, -122.41, 1L);
        drone.setStatus(DroneStatus.EN_ROUTE_DELIVERY);
        drone.setBatteryPercent(100.0);
        drone.setCurrentPayloadKg(0.0);

        double emptyBattery = batteryDrainModel.updateBattery(drone, 60.0, 0.0);

        drone.setBatteryPercent(100.0);
        drone.setCurrentPayloadKg(3.0); // full payload
        double fullPayloadBattery = batteryDrainModel.updateBattery(drone, 60.0, 0.0);

        assertTrue(emptyBattery < 100.0, "Drone should consume battery during flight");
        assertTrue(fullPayloadBattery < emptyBattery, "Loaded drone should drain battery faster than empty drone");
    }

    @Test
    public void testBatteryChargingAtDepot() {
        Drone drone = new Drone("TEST-01", DroneModelType.QUADCOPTER_STANDARD, 37.77, -122.41, 1L);
        drone.setStatus(DroneStatus.CHARGING);
        drone.setBatteryPercent(50.0);

        double charged = batteryDrainModel.updateBattery(drone, 10.0, 0.0);
        // 3.0% per sec * 10s = 30% added => 80.0%
        assertEquals(80.0, charged, 0.1, "Battery should recharge at 3% per sec");
    }
}
