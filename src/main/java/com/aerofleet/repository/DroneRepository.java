package com.aerofleet.repository;

import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.model.Drone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DroneRepository extends JpaRepository<Drone, Long> {
    List<Drone> findByStatus(DroneStatus status);
    List<Drone> findByCurrentDepotId(Long depotId);
    long countByStatus(DroneStatus status);
}
