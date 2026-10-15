package com.aerofleet.repository;

import com.aerofleet.domain.model.NoFlyZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoFlyZoneRepository extends JpaRepository<NoFlyZone, Long> {
    List<NoFlyZone> findByActiveTrue();
}
