package com.aerofleet.service;

import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.model.DeliveryOrder;
import com.aerofleet.domain.model.Depot;
import com.aerofleet.domain.model.Drone;
import com.aerofleet.domain.model.Location;
import com.aerofleet.domain.model.NoFlyZone;
import com.aerofleet.domain.model.Waypoint;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generates simple 2D waypoint routes for drone delivery missions.
 */
@Service
public class RoutingService {

    private final GeofenceService geofenceService;

    public RoutingService(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    /**
     * Builds a flight route on the 2D grid: Depot -> Customer -> Depot
     */
    public List<Waypoint> buildDeliveryRoute(Drone drone, Depot depot, DeliveryOrder order) {
        List<Waypoint> route = new ArrayList<>();
        double cruiseSpeed = drone.getModelType().getMaxSpeedMps();

        Location depotLoc = depot.getLocation();
        Location dropLoc = order.getDeliveryLocation();

        // 1. Outbound route to Customer (checking for No-Fly Zone obstacle)
        Optional<NoFlyZone> outboundObstacle = geofenceService.getIntersectedZone(depotLoc, dropLoc);
        if (outboundObstacle.isPresent()) {
            Location bypass = geofenceService.calculateBypassWaypoint(depotLoc, dropLoc, outboundObstacle.get());
            route.add(new Waypoint(bypass, cruiseSpeed * 0.9, DroneStatus.EN_ROUTE_DELIVERY, "Detour around " + outboundObstacle.get().getName()));
        }
        route.add(new Waypoint(dropLoc, cruiseSpeed, DroneStatus.EN_ROUTE_DELIVERY, "Cruising to customer destination"));

        // 2. Deliver package at destination
        route.add(new Waypoint(dropLoc, 0.0, DroneStatus.DELIVERING, "Releasing payload to customer"));

        // 3. Return leg back to depot
        Optional<NoFlyZone> returnObstacle = geofenceService.getIntersectedZone(dropLoc, depotLoc);
        if (returnObstacle.isPresent()) {
            Location bypass = geofenceService.calculateBypassWaypoint(dropLoc, depotLoc, returnObstacle.get());
            route.add(new Waypoint(bypass, cruiseSpeed * 0.9, DroneStatus.RETURNING, "Return detour around " + returnObstacle.get().getName()));
        }
        route.add(new Waypoint(depotLoc, cruiseSpeed, DroneStatus.RETURNING, "Returning to charging depot"));

        // 4. Landing at Depot
        route.add(new Waypoint(depotLoc, cruiseSpeed * 0.4, DroneStatus.LANDING, "Landing onto charging pad"));

        return route;
    }

    /**
     * Emergency Return-To-Home (RTH) route directly to nearest depot.
     */
    public List<Waypoint> buildEmergencyRthRoute(Drone drone, Depot depot) {
        List<Waypoint> route = new ArrayList<>();
        double speed = drone.getModelType().getMaxSpeedMps() * 0.8;
        Location depotLoc = depot.getLocation();

        route.add(new Waypoint(depotLoc, speed, DroneStatus.EMERGENCY_RTH, "Emergency Return-To-Home initiated"));
        route.add(new Waypoint(depotLoc, speed * 0.3, DroneStatus.LANDING, "Emergency touchdown on depot pad"));

        return route;
    }
}
