package com.aerofleet.controller;

import com.aerofleet.domain.enums.OrderStatus;
import com.aerofleet.domain.model.DeliveryOrder;
import com.aerofleet.domain.model.Depot;
import com.aerofleet.dto.AlertEventDTO;
import com.aerofleet.dto.CreateOrderRequest;
import com.aerofleet.repository.DeliveryOrderRepository;
import com.aerofleet.repository.DepotRepository;
import com.aerofleet.simulation.SimulationEngine;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * REST API for managing delivery orders on the 2D grid.
 */
@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final DeliveryOrderRepository orderRepository;
    private final DepotRepository depotRepository;
    private final SimulationEngine simulationEngine;
    private final TelemetryBroadcaster telemetryBroadcaster;
    private final Random random = new Random();

    public OrderController(
            DeliveryOrderRepository orderRepository,
            DepotRepository depotRepository,
            SimulationEngine simulationEngine,
            TelemetryBroadcaster telemetryBroadcaster) {
        this.orderRepository = orderRepository;
        this.depotRepository = depotRepository;
        this.simulationEngine = simulationEngine;
        this.telemetryBroadcaster = telemetryBroadcaster;
    }

    @GetMapping
    public List<DeliveryOrder> getAllOrders(@RequestParam(required = false) OrderStatus status) {
        if (status != null) {
            return orderRepository.findByStatusOrderByCreatedAtAsc(status);
        }
        return orderRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<DeliveryOrder> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        String orderNum = "ORD-" + (System.currentTimeMillis() % 100000);
        DeliveryOrder order = new DeliveryOrder(
                orderNum,
                request.getCustomerName(),
                request.getDeliveryX(),
                request.getDeliveryY(),
                request.getPackageWeightKg(),
                request.getPriority(),
                request.getOriginDepotId()
        );

        DeliveryOrder saved = orderRepository.save(order);

        telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                "INFO", "Order Created",
                "Order #" + orderNum + " for " + request.getCustomerName() + " at (" + (int)order.getDeliveryX() + ", " + (int)order.getDeliveryY() + ")",
                null, saved.getId()
        ));

        // Trigger instant in-memory dispatch
        simulationEngine.triggerDispatch();

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/simulate-batch")
    public ResponseEntity<List<DeliveryOrder>> simulateBatchOrders(@RequestParam(defaultValue = "5") int count) {
        List<Depot> depots = depotRepository.findAll();
        if (depots.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String[] customers = {
                "Campus Health Center", "Engineering Block B", "Science Library",
                "North Dormitory", "Student Union Complex", "Athletic Pavilion",
                "Robotics Innovation Lab", "South Cafeteria"
        };

        String[] priorities = {"STANDARD", "EXPRESS", "MEDICAL"};

        List<DeliveryOrder> createdOrders = new ArrayList<>();

        for (int i = 0; i < Math.min(count, 15); i++) {
            Depot baseDepot = depots.get(random.nextInt(depots.size()));

            // Random coordinates on grid [80, 720] x [80, 520]
            double targetX = 80 + random.nextInt(640);
            double targetY = 80 + random.nextInt(440);

            String customer = customers[random.nextInt(customers.length)];
            String priority = priorities[random.nextInt(priorities.length)];
            double weight = Math.round((0.5 + (random.nextDouble() * 4.0)) * 10.0) / 10.0;
            String orderNum = "ORD-" + (System.currentTimeMillis() % 100000) + "-" + (i + 1);

            DeliveryOrder order = new DeliveryOrder(
                    orderNum, customer, targetX, targetY, weight, priority, baseDepot.getId()
            );

            createdOrders.add(orderRepository.save(order));
        }

        telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                "INFO", "Batch Orders Queued",
                "Queued " + createdOrders.size() + " orders across the grid map.",
                null, null
        ));

        simulationEngine.triggerDispatch();

        return ResponseEntity.ok(createdOrders);
    }
}
