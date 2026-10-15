package com.aerofleet.controller;

import com.aerofleet.domain.model.Drone;
import com.aerofleet.repository.DroneRepository;
import com.aerofleet.simulation.SimulationEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drones")
@CrossOrigin(origins = "*")
public class DroneController {

    private final DroneRepository droneRepository;
    private final SimulationEngine simulationEngine;

    public DroneController(DroneRepository droneRepository, SimulationEngine simulationEngine) {
        this.droneRepository = droneRepository;
        this.simulationEngine = simulationEngine;
    }

    @GetMapping
    public List<Drone> getAllDrones() {
        return droneRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Drone> getDroneById(@PathVariable Long id) {
        return droneRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/recall")
    public ResponseEntity<String> recallDrone(@PathVariable Long id) {
        return droneRepository.findById(id).map(drone -> {
            simulationEngine.triggerEmergencyRth(drone, "Manual operator recall");
            return ResponseEntity.ok("Drone " + drone.getCallsign() + " recalled to nearest depot.");
        }).orElse(ResponseEntity.notFound().build());
    }
}
