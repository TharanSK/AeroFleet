package com.aerofleet.controller;

import com.aerofleet.domain.model.NoFlyZone;
import com.aerofleet.dto.AlertEventDTO;
import com.aerofleet.dto.CreateZoneRequest;
import com.aerofleet.repository.NoFlyZoneRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST API for managing restricted airspace (No-Fly Zones) on the 2D grid.
 */
@RestController
@RequestMapping("/api/v1/geofence")
@CrossOrigin(origins = "*")
public class GeofenceController {

    private final NoFlyZoneRepository noFlyZoneRepository;
    private final TelemetryBroadcaster telemetryBroadcaster;

    public GeofenceController(
            NoFlyZoneRepository noFlyZoneRepository,
            TelemetryBroadcaster telemetryBroadcaster) {
        this.noFlyZoneRepository = noFlyZoneRepository;
        this.telemetryBroadcaster = telemetryBroadcaster;
    }

    @GetMapping
    public List<NoFlyZone> getNoFlyZones() {
        return noFlyZoneRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<NoFlyZone> createNoFlyZone(@Valid @RequestBody CreateZoneRequest request) {
        NoFlyZone zone = new NoFlyZone(
                null,
                request.getName(),
                request.getCenterX(),
                request.getCenterY(),
                request.getRadius(),
                request.isActive()
        );
        NoFlyZone saved = noFlyZoneRepository.save(zone);

        telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                "WARN",
                "Restricted Zone Added",
                "No-Fly Zone '" + saved.getName() + "' established at (" + (int) saved.getCenterX() + ", " + (int) saved.getCenterY() + ") with radius " + (int) saved.getRadius() + "u",
                null, null
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNoFlyZone(@PathVariable Long id) {
        return noFlyZoneRepository.findById(id).map(zone -> {
            noFlyZoneRepository.delete(zone);
            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    "INFO",
                    "Zone Decommissioned",
                    "No-Fly Zone '" + zone.getName() + "' has been removed from airspace registry",
                    null, null
            ));
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllZones() {
        long count = noFlyZoneRepository.count();
        noFlyZoneRepository.deleteAll();
        telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                "INFO",
                "Airspace Cleared",
                "All " + count + " restricted No-Fly Zones have been decommissioned and removed",
                null, null
        ));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<NoFlyZone> toggleNoFlyZone(@PathVariable Long id) {
        return noFlyZoneRepository.findById(id).map(zone -> {
            zone.setActive(!zone.isActive());
            NoFlyZone updated = noFlyZoneRepository.save(zone);
            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    updated.isActive() ? "WARN" : "INFO",
                    updated.isActive() ? "Zone Activated" : "Zone Deactivated",
                    "No-Fly Zone '" + updated.getName() + "' is now " + (updated.isActive() ? "ACTIVE (Avoidance Engaged)" : "INACTIVE"),
                    null, null
            ));
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }
}
