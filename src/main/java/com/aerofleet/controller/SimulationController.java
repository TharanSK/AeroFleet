package com.aerofleet.controller;

import com.aerofleet.domain.enums.WeatherCondition;
import com.aerofleet.dto.AlertEventDTO;
import com.aerofleet.dto.SimulationControlDTO;
import com.aerofleet.service.WeatherService;
import com.aerofleet.simulation.SimulationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private final SimulationEngine simulationEngine;
    private final WeatherService weatherService;
    private final TelemetryBroadcaster telemetryBroadcaster;

    public SimulationController(
            SimulationEngine simulationEngine,
            WeatherService weatherService,
            TelemetryBroadcaster telemetryBroadcaster) {
        this.simulationEngine = simulationEngine;
        this.weatherService = weatherService;
        this.telemetryBroadcaster = telemetryBroadcaster;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "running", simulationEngine.isRunning(),
                "speedMultiplier", simulationEngine.getSpeedMultiplier(),
                "simTimeElapsedSeconds", simulationEngine.getSimTimeElapsedSeconds(),
                "weather", weatherService.getCondition().name(),
                "windSpeedMps", weatherService.getWindSpeedMps(),
                "windDirectionDeg", weatherService.getWindDirectionDeg()
        ));
    }

    @PostMapping("/control")
    public ResponseEntity<String> updateControl(@RequestBody SimulationControlDTO control) {
        if (control.getRunning() != null) {
            simulationEngine.setRunning(control.getRunning());
            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    "INFO", "Simulation " + (control.getRunning() ? "Resumed" : "Paused"),
                    "Simulation state changed by operator.", null, null
            ));
        }

        if (control.getSpeedMultiplier() != null) {
            simulationEngine.setSpeedMultiplier(control.getSpeedMultiplier());
        }

        boolean envChanged = false;
        if (control.getWeatherCondition() != null) {
            try {
                WeatherCondition condition = WeatherCondition.valueOf(control.getWeatherCondition().toUpperCase());
                weatherService.setCondition(condition);
                if (control.getWindSpeedMps() == null) {
                    if (condition == WeatherCondition.STORM) {
                        weatherService.setWindSpeedMps(18.0);
                    } else if (condition == WeatherCondition.WINDY) {
                        weatherService.setWindSpeedMps(10.0);
                    } else {
                        weatherService.setWindSpeedMps(4.0);
                    }
                }
                envChanged = true;
            } catch (IllegalArgumentException ignored) {}
        }

        if (control.getWindSpeedMps() != null) {
            weatherService.setWindSpeedMps(control.getWindSpeedMps());
            envChanged = true;
        }

        if (control.getWindDirectionDeg() != null) {
            weatherService.setWindDirectionDeg(control.getWindDirectionDeg());
            envChanged = true;
        }

        if (envChanged) {
            WeatherCondition condition = weatherService.getCondition();
            double speed = weatherService.getWindSpeedMps();
            double dir = weatherService.getWindDirectionDeg();
            String cardinal = getCardinalDirection(dir);
            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    condition == WeatherCondition.STORM ? "DANGER" : (speed > 12.0 ? "WARN" : "INFO"),
                    "Atmosphere Updated",
                    String.format("Weather: %s | Wind: %.1f m/s @ %.0f° (%s)", condition.name(), speed, dir, cardinal),
                    null, null
            ));
        }

        return ResponseEntity.ok("Simulation control updated successfully");
    }

    private String getCardinalDirection(double deg) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        int idx = (int) Math.round(((deg % 360.0) / 45.0));
        return directions[Math.max(0, Math.min(8, idx))];
    }

    @PostMapping("/incident")
    public ResponseEntity<String> injectIncident(
            @RequestParam Long droneId,
            @RequestParam String type,
            @RequestParam(defaultValue = "30.0") double magnitude) {
        simulationEngine.injectIncident(droneId, type, magnitude);
        return ResponseEntity.ok("Incident " + type + " injected on drone #" + droneId);
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetSimulation() {
        simulationEngine.resetSimulation();
        return ResponseEntity.ok("Simulation time reset");
    }
}
