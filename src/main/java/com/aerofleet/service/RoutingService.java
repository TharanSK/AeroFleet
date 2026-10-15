package com.aerofleet.service;

import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.model.DeliveryOrder;
import com.aerofleet.domain.model.Depot;
import com.aerofleet.domain.model.Drone;
import com.aerofleet.domain.model.GeoCoordinate;
import com.aerofleet.domain.model.NoFlyZone;
import com.aerofleet.domain.model.Waypoint;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RoutingService {

    private final GeofenceService geofenceService;

    public RoutingService(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    /**
     * Builds a full mission flight plan from Depot to Customer Delivery Point and return.
     */
    public List<Waypoint> buildDeliveryRoute(Drone drone, Depot depot, DeliveryOrder order) {
        List<Waypoint> route = new ArrayList<>();
        double cruiseAlt = drone.getModelType().getCruiseAltitudeMeters();
        double maxSpeed = drone.getModelType().getMaxSpeedMps();

        GeoCoordinate depotGround = depot.toCoordinate();
        GeoCoordinate depotAir = new GeoCoordinate(depot.getLatitude(), depot.getLongitude(), cruiseAlt);

        GeoCoordinate dropGround = order.getDeliveryCoordinate();
        GeoCoordinate dropAir = new GeoCoordinate(dropGround.getLatitude(), dropGround.getLongitude(), cruiseAlt);
        GeoCoordinate dropHover = new GeoCoordinate(dropGround.getLatitude(), dropGround.getLongitude(), 3.0);

        // 1. TAKEOFF: Vertical climb to cruise altitude
        route.add(new Waypoint(depotAir, maxSpeed * 0.4, DroneStatus.TAKING_OFF, "Ascending to cruise altitude"));

        // 2. EN_ROUTE_DELIVERY: Cruise to drop coordinates (handling NFZ avoidance)
        Optional<NoFlyZone> outboundObstacle = geofenceService.getIntersectedZone(depotAir, dropAir);
        if (outboundObstacle.isPresent()) {
            GeoCoordinate bypass = geofenceService.calculateBypassWaypoint(depotAir, dropAir, outboundObstacle.get());
            route.add(new Waypoint(bypass, maxSpeed * 0.9, DroneStatus.EN_ROUTE_DELIVERY, "Airspace detour around " + outboundObstacle.get().getName()));
        }
        route.add(new Waypoint(dropAir, maxSpeed, DroneStatus.EN_ROUTE_DELIVERY, "Cruising to delivery coordinates"));

        // 3. DESCENDING: Descent to drop height
        route.add(new Waypoint(dropHover, maxSpeed * 0.3, DroneStatus.DESCENDING_DELIVERY, "Descending for package delivery"));

        // 4. DELIVERING: Hover point for package release
        route.add(new Waypoint(dropHover, 0.0, DroneStatus.DELIVERING, "Releasing payload to customer"));

        // 5. ASCENDING: Climb back to cruise altitude
        route.add(new Waypoint(dropAir, maxSpeed * 0.4, DroneStatus.ASCENDING_DELIVERY, "Ascending to cruise altitude"));

        // 6. RETURNING: Cruise back to depot
        Optional<NoFlyZone> returnObstacle = geofenceService.getIntersectedZone(dropAir, depotAir);
        if (returnObstacle.isPresent()) {
            GeoCoordinate bypass = geofenceService.calculateBypassWaypoint(dropAir, depotAir, returnObstacle.get());
            route.add(new Waypoint(bypass, maxSpeed * 0.9, DroneStatus.RETURNING, "Return detour around " + returnObstacle.get().getName()));
        }
        route.add(new Waypoint(depotAir, maxSpeed, DroneStatus.RETURNING, "Returning to depot air corridor"));

        // 7. LANDING: Vertical touchdown onto depot pad
        route.add(new Waypoint(depotGround, maxSpeed * 0.3, DroneStatus.LANDING, "Landing onto charging pad"));

        return route;
    }

    /**
     * Generates an emergency Return-To-Home flight plan back to the safe depot.
     */
    public List<Waypoint> buildEmergencyRthRoute(Drone drone, Depot depot) {
        List<Waypoint> route = new ArrayList<>();
        double cruiseAlt = Math.max(drone.getCurrentAltitudeMeters(), drone.getModelType().getCruiseAltitudeMeters());
        double speed = drone.getModelType().getMaxSpeedMps() * 0.8;

        GeoCoordinate currentCoord = drone.getCurrentCoordinate();
        GeoCoordinate depotAir = new GeoCoordinate(depot.getLatitude(), depot.getLongitude(), cruiseAlt);
        GeoCoordinate depotGround = depot.toCoordinate();

        // 1. Immediate vector to depot airspace
        route.add(new Waypoint(depotAir, speed, DroneStatus.EMERGENCY_RTH, "Emergency Return-To-Home initiated"));

        // 2. Direct emergency landing
        route.add(new Waypoint(depotGround, speed * 0.3, DroneStatus.LANDING, "Emergency touchdown on depot pad"));

        return route;
    }
}
