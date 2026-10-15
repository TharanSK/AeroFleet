package com.aerofleet.controller;

import com.aerofleet.domain.model.Depot;
import com.aerofleet.repository.DepotRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/depots")
@CrossOrigin(origins = "*")
public class DepotController {

    private final DepotRepository depotRepository;

    public DepotController(DepotRepository depotRepository) {
        this.depotRepository = depotRepository;
    }

    @GetMapping
    public List<Depot> getAllDepots() {
        return depotRepository.findAll();
    }
}
