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
 * Generates 2D waypoint routes for drone delivery missions with multi-zone obstacle avoidance.
 */
@Service
public class RoutingService {

    private final GeofenceService geofenceService;

    public RoutingService(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    /**
     * Recursively computes an obstacle-free sequence of coordinates from start to end,
     * detouring around any active circular No-Fly Zones.
     */
    public List<Location> planClearPath(Location start, Location end, int depth) {
        List<Location> points = new ArrayList<>();
        if (depth >= 4) { // Prevent infinite recursion in overly congested airspace
            points.add(end);
            return points;
        }

        Optional<NoFlyZone> obstacleOpt = geofenceService.getClosestIntersectedZone(start, end);
        if (obstacleOpt.isPresent()) {
            NoFlyZone obstacle = obstacleOpt.get();
            Location bypass = geofenceService.calculateBypassWaypoint(start, end, obstacle);

            // Recursively plan from start to bypass, and from bypass to end
            List<Location> firstLeg = planClearPath(start, bypass, depth + 1);
            List<Location> secondLeg = planClearPath(bypass, end, depth + 1);

            points.addAll(firstLeg);
            points.addAll(secondLeg);
        } else {
            points.add(end);
        }
        return points;
    }

    /**
     * Builds a complete flight route on the 2D grid: Depot -> Detours -> Customer -> Deliver -> Detours -> Depot -> Land
     */
    public List<Waypoint> buildDeliveryRoute(Drone drone, Depot depot, DeliveryOrder order) {
        List<Waypoint> route = new ArrayList<>();
        double cruiseSpeed = drone.getModelType().getMaxSpeedMps();

        Location depotLoc = depot.getLocation();
        Location dropLoc = order.getDeliveryLocation();

        // 1. Outbound route to Customer (checking for any active No-Fly Zones)
        List<Location> outboundPoints = planClearPath(depotLoc, dropLoc, 0);
        for (int i = 0; i < outboundPoints.size() - 1; i++) {
            route.add(new Waypoint(outboundPoints.get(i), cruiseSpeed * 0.9, DroneStatus.EN_ROUTE_DELIVERY, "Detour around restricted airspace"));
        }
        route.add(new Waypoint(dropLoc, cruiseSpeed, DroneStatus.EN_ROUTE_DELIVERY, "Cruising to customer destination"));

        // 2. Deliver package at destination
        route.add(new Waypoint(dropLoc, 0.0, DroneStatus.DELIVERING, "Releasing payload to customer"));

        // 3. Return leg back to depot (checking for any active No-Fly Zones)
        List<Location> returnPoints = planClearPath(dropLoc, depotLoc, 0);
        for (int i = 0; i < returnPoints.size() - 1; i++) {
            route.add(new Waypoint(returnPoints.get(i), cruiseSpeed * 0.9, DroneStatus.RETURNING, "Return detour around restricted airspace"));
        }
        route.add(new Waypoint(depotLoc, cruiseSpeed, DroneStatus.RETURNING, "Returning to charging depot"));

        // 4. Landing at Depot
        route.add(new Waypoint(depotLoc, cruiseSpeed * 0.4, DroneStatus.LANDING, "Landing onto charging pad"));

        return route;
    }

    /**
     * Re-plans remaining flight waypoints for an in-flight drone to bypass newly placed/toggled zones.
     */
    public List<Waypoint> replanInFlightDrone(Drone drone, DeliveryOrder order, Depot depot) {
        List<Waypoint> newRoute = new ArrayList<>();
        double cruiseSpeed = drone.getModelType().getMaxSpeedMps();
        Location currentLoc = drone.getLocation();
        Location depotLoc = depot.getLocation();

        if (drone.getStatus() == DroneStatus.EN_ROUTE_DELIVERY && order != null) {
            Location dropLoc = order.getDeliveryLocation();

            // Outbound remaining path: current location -> customer
            List<Location> outboundPoints = planClearPath(currentLoc, dropLoc, 0);
            for (int i = 0; i < outboundPoints.size() - 1; i++) {
                newRoute.add(new Waypoint(outboundPoints.get(i), cruiseSpeed * 0.9, DroneStatus.EN_ROUTE_DELIVERY, "Dynamic detour around new restricted airspace"));
            }
            newRoute.add(new Waypoint(dropLoc, cruiseSpeed, DroneStatus.EN_ROUTE_DELIVERY, "Cruising to customer destination"));
            newRoute.add(new Waypoint(dropLoc, 0.0, DroneStatus.DELIVERING, "Releasing payload to customer"));

            // Return leg: customer -> depot
            List<Location> returnPoints = planClearPath(dropLoc, depotLoc, 0);
            for (int i = 0; i < returnPoints.size() - 1; i++) {
                newRoute.add(new Waypoint(returnPoints.get(i), cruiseSpeed * 0.9, DroneStatus.RETURNING, "Return detour around restricted airspace"));
            }
            newRoute.add(new Waypoint(depotLoc, cruiseSpeed, DroneStatus.RETURNING, "Returning to charging depot"));
            newRoute.add(new Waypoint(depotLoc, cruiseSpeed * 0.4, DroneStatus.LANDING, "Landing onto charging pad"));

        } else if (drone.getStatus() == DroneStatus.RETURNING || drone.getStatus() == DroneStatus.EMERGENCY_RTH) {
            // Return remaining path: current location -> depot
            List<Location> returnPoints = planClearPath(currentLoc, depotLoc, 0);
            for (int i = 0; i < returnPoints.size() - 1; i++) {
                newRoute.add(new Waypoint(returnPoints.get(i), cruiseSpeed * 0.9, drone.getStatus(), "Dynamic detour to depot"));
            }
            newRoute.add(new Waypoint(depotLoc, cruiseSpeed, drone.getStatus(), "Returning to charging depot"));
            newRoute.add(new Waypoint(depotLoc, cruiseSpeed * 0.4, DroneStatus.LANDING, "Landing onto charging pad"));
        }

        return newRoute;
    }

    /**
     * Checks if any segment of the remaining flight route intersects an active restricted zone.
     */
    public boolean remainingRouteHasZoneIntersection(Drone drone) {
        if (drone.getWaypoints() == null || drone.getWaypoints().isEmpty()) return false;
        Location current = drone.getLocation();
        int idx = drone.getCurrentWaypointIndex();
        for (int i = idx; i < drone.getWaypoints().size(); i++) {
            Location next = drone.getWaypoints().get(i).getLocation();
            if (geofenceService.getClosestIntersectedZone(current, next).isPresent()) {
                return true;
            }
            current = next;
        }
        return false;
    }

    /**
     * Emergency Return-To-Home (RTH) route directly to nearest depot with obstacle avoidance.
     */
    public List<Waypoint> buildEmergencyRthRoute(Drone drone, Depot depot) {
        List<Waypoint> route = new ArrayList<>();
        double speed = drone.getModelType().getMaxSpeedMps() * 0.8;
        Location currentLoc = drone.getLocation();
        Location depotLoc = depot.getLocation();

        List<Location> rthPoints = planClearPath(currentLoc, depotLoc, 0);
        for (int i = 0; i < rthPoints.size() - 1; i++) {
            route.add(new Waypoint(rthPoints.get(i), speed * 0.9, DroneStatus.EMERGENCY_RTH, "Emergency RTH detour"));
        }
        route.add(new Waypoint(depotLoc, speed, DroneStatus.EMERGENCY_RTH, "Emergency Return-To-Home initiated"));
        route.add(new Waypoint(depotLoc, speed * 0.3, DroneStatus.LANDING, "Emergency touchdown on depot pad"));

        return route;
    }
}
