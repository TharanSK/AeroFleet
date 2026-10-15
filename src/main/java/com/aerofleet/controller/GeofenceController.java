package com.aerofleet.controller;

import com.aerofleet.domain.model.NoFlyZone;
import com.aerofleet.repository.NoFlyZoneRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/geofence")
@CrossOrigin(origins = "*")
public class GeofenceController {

    private final NoFlyZoneRepository noFlyZoneRepository;

    public GeofenceController(NoFlyZoneRepository noFlyZoneRepository) {
        this.noFlyZoneRepository = noFlyZoneRepository;
    }

    @GetMapping
    public List<NoFlyZone> getNoFlyZones() {
        return noFlyZoneRepository.findAll();
    }
}
