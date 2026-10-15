package com.aerofleet;

import com.aerofleet.repository.DepotRepository;
import com.aerofleet.repository.DroneRepository;
import com.aerofleet.repository.NoFlyZoneRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AeroFleetApplicationTests {

    @Autowired
    private DroneRepository droneRepository;

    @Autowired
    private DepotRepository depotRepository;

    @Autowired
    private NoFlyZoneRepository noFlyZoneRepository;

    @Test
    void contextLoads() {
        assertNotNull(droneRepository);
        assertNotNull(depotRepository);
        assertNotNull(noFlyZoneRepository);

        assertTrue(droneRepository.count() > 0, "Drones should be seeded");
        assertTrue(depotRepository.count() > 0, "Depots should be seeded");
        assertTrue(noFlyZoneRepository.count() > 0, "No-Fly Zones should be seeded");
    }
}
