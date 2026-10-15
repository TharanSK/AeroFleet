package com.aerofleet.service;

import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.enums.OrderStatus;
import com.aerofleet.domain.model.DeliveryOrder;
import com.aerofleet.domain.model.Depot;
import com.aerofleet.domain.model.Drone;
import com.aerofleet.domain.model.Waypoint;
import com.aerofleet.repository.DeliveryOrderRepository;
import com.aerofleet.repository.DepotRepository;
import com.aerofleet.repository.DroneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Intelligent Dispatch Engine: Matches pending orders to optimal drones on the grid.
 * Demonstrates Collections, Stream API, and Greedy Scheduling.
 */
@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private final DroneRepository droneRepository;
    private final DeliveryOrderRepository orderRepository;
    private final DepotRepository depotRepository;
    private final RoutingService routingService;

    public DispatchService(
            DroneRepository droneRepository,
            DeliveryOrderRepository orderRepository,
            DepotRepository depotRepository,
            RoutingService routingService) {
        this.droneRepository = droneRepository;
        this.orderRepository = orderRepository;
        this.depotRepository = depotRepository;
        this.routingService = routingService;
    }

    /**
     * Matches pending orders directly to the provided live in-memory drones.
     */
    @Transactional
    public synchronized int dispatchPendingOrders(Collection<Drone> candidateDrones) {
        List<DeliveryOrder> pendingOrders = orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING);
        if (pendingOrders.isEmpty() || candidateDrones == null || candidateDrones.isEmpty()) {
            return 0;
        }

        List<Depot> allDepots = depotRepository.findAll();
        int dispatchedCount = 0;

        for (DeliveryOrder order : pendingOrders) {
            Optional<Drone> bestDroneOpt = findOptimalDrone(order, candidateDrones, allDepots);

            if (bestDroneOpt.isPresent()) {
                Drone drone = bestDroneOpt.get();
                Depot depot = allDepots.stream()
                        .filter(d -> d.getId().equals(drone.getCurrentDepotId()))
                        .findFirst()
                        .orElse(allDepots.get(0));

                // Generate 2D route waypoints
                List<Waypoint> route = routingService.buildDeliveryRoute(drone, depot, order);

                drone.setWaypoints(route);
                drone.setStatus(DroneStatus.EN_ROUTE_DELIVERY);
                drone.setCurrentPayloadKg(order.getPackageWeightKg());
                drone.setAssignedOrderId(order.getId());

                order.setStatus(OrderStatus.IN_TRANSIT);
                order.setAssignedDroneId(drone.getId());
                order.setOriginDepotId(depot.getId());

                droneRepository.save(drone);
                orderRepository.save(order);

                log.info("Dispatched Order [{}] ({}) to Drone [{}] at Depot [{}] with {} waypoints",
                        order.getOrderNumber(), order.getCustomerName(), drone.getCallsign(), depot.getName(), route.size());
                dispatchedCount++;
            }
        }
        return dispatchedCount;
    }

    @Transactional
    public synchronized int dispatchPendingOrders() {
        return dispatchPendingOrders(droneRepository.findAll());
    }

    private Optional<Drone> findOptimalDrone(DeliveryOrder order, Collection<Drone> drones, List<Depot> depots) {
        return drones.stream()
                .filter(d -> d.getStatus() == DroneStatus.IDLE ||
                            (d.getStatus() == DroneStatus.CHARGING && d.getBatteryPercent() >= 70.0))
                .filter(d -> d.getModelType().getMaxPayloadKg() >= order.getPackageWeightKg())
                .filter(d -> hasSufficientBattery(d, order, depots))
                .min(Comparator.comparingDouble(d -> calculateCost(d, order, depots)));
    }

    private boolean hasSufficientBattery(Drone drone, DeliveryOrder order, List<Depot> depots) {
        Depot depot = depots.stream()
                .filter(d -> d.getId().equals(drone.getCurrentDepotId()))
                .findFirst()
                .orElse(null);
        if (depot == null) return false;

        double roundtripDistance = depot.getLocation().distanceTo(order.getDeliveryLocation()) * 2.0;
        // ~1% battery per 15 grid units
        double estimatedBatteryDrain = (roundtripDistance / 15.0);
        return drone.getBatteryPercent() >= (estimatedBatteryDrain + 15.0); // 15% safety buffer
    }

    private double calculateCost(Drone drone, DeliveryOrder order, List<Depot> depots) {
        Depot depot = depots.stream()
                .filter(d -> d.getId().equals(drone.getCurrentDepotId()))
                .findFirst()
                .orElse(null);
        if (depot == null) return Double.MAX_VALUE;

        double distance = depot.getLocation().distanceTo(order.getDeliveryLocation());
        return distance - (drone.getBatteryPercent() * 2.0);
    }
}
