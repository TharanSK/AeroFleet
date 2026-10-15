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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
     * Attempts to match all pending delivery orders with available, optimal drones.
     */
    @Transactional
    public synchronized int dispatchPendingOrders() {
        List<DeliveryOrder> pendingOrders = orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            return 0;
        }

        List<Drone> allDrones = droneRepository.findAll();
        List<Depot> allDepots = depotRepository.findAll();
        int dispatchedCount = 0;

        for (DeliveryOrder order : pendingOrders) {
            Optional<Drone> bestDroneOpt = findOptimalDrone(order, allDrones, allDepots);

            if (bestDroneOpt.isPresent()) {
                Drone drone = bestDroneOpt.get();
                Depot depot = allDepots.stream()
                        .filter(d -> d.getId().equals(drone.getCurrentDepotId()))
                        .findFirst()
                        .orElse(allDepots.get(0));

                // Generate full flight route
                List<Waypoint> route = routingService.buildDeliveryRoute(drone, depot, order);

                drone.setWaypoints(route);
                drone.setStatus(DroneStatus.ASSIGNED);
                drone.setCurrentPayloadKg(order.getPackageWeightKg());
                drone.setAssignedOrderId(order.getId());

                order.setStatus(OrderStatus.ASSIGNED);
                order.setAssignedDroneId(drone.getId());
                order.setOriginDepotId(depot.getId());

                droneRepository.save(drone);
                orderRepository.save(order);

                log.info("Dispatched Order [{}] ({}) to Drone [{}] at Depot [{}]",
                        order.getOrderNumber(), order.getCustomerName(), drone.getCallsign(), depot.getName());
                dispatchedCount++;
            }
        }
        return dispatchedCount;
    }

    private Optional<Drone> findOptimalDrone(DeliveryOrder order, List<Drone> drones, List<Depot> depots) {
        return drones.stream()
                .filter(d -> d.getStatus() == DroneStatus.IDLE ||
                            (d.getStatus() == DroneStatus.CHARGING && d.getBatteryPercent() >= 75.0))
                .filter(d -> d.getModelType().getMaxPayloadKg() >= order.getPackageWeightKg())
                .filter(d -> hasSufficientRange(d, order, depots))
                .min(Comparator.comparingDouble(d -> calculateDispatchCost(d, order, depots)));
    }

    private boolean hasSufficientRange(Drone drone, DeliveryOrder order, List<Depot> depots) {
        Depot depot = depots.stream()
                .filter(d -> d.getId().equals(drone.getCurrentDepotId()))
                .findFirst()
                .orElse(null);
        if (depot == null) return false;

        double roundtripDistanceMeters = depot.toCoordinate().distanceTo(order.getDeliveryCoordinate()) * 2.2; // with buffers
        double cruiseSpeed = drone.getModelType().getMaxSpeedMps();
        double estimatedFlightTimeSeconds = roundtripDistanceMeters / cruiseSpeed;

        // Approximate 1% battery per 15-20 seconds under payload
        double estimatedBatteryDrain = (estimatedFlightTimeSeconds / 18.0) * (1.0 + (order.getPackageWeightKg() / 5.0));
        double requiredBattery = estimatedBatteryDrain + 20.0; // 20% safety margin

        return drone.getBatteryPercent() >= requiredBattery;
    }

    private double calculateDispatchCost(Drone drone, DeliveryOrder order, List<Depot> depots) {
        Depot depot = depots.stream()
                .filter(d -> d.getId().equals(drone.getCurrentDepotId()))
                .findFirst()
                .orElse(null);
        if (depot == null) return Double.MAX_VALUE;

        double dist = depot.toCoordinate().distanceTo(order.getDeliveryCoordinate());
        // Prefer higher battery, shorter distance, and matching payload size
        return dist - (drone.getBatteryPercent() * 20.0) + (drone.getModelType().getMaxPayloadKg() * 10.0);
    }
}
