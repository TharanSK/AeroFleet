package com.aerofleet.controller;

import com.aerofleet.domain.enums.OrderStatus;
import com.aerofleet.domain.model.DeliveryOrder;
import com.aerofleet.domain.model.Depot;
import com.aerofleet.dto.AlertEventDTO;
import com.aerofleet.dto.CreateOrderRequest;
import com.aerofleet.repository.DeliveryOrderRepository;
import com.aerofleet.repository.DepotRepository;
import com.aerofleet.service.DispatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final DeliveryOrderRepository orderRepository;
    private final DepotRepository depotRepository;
    private final DispatchService dispatchService;
    private final TelemetryBroadcaster telemetryBroadcaster;
    private final Random random = new Random();

    public OrderController(
            DeliveryOrderRepository orderRepository,
            DepotRepository depotRepository,
            DispatchService dispatchService,
            TelemetryBroadcaster telemetryBroadcaster) {
        this.orderRepository = orderRepository;
        this.depotRepository = depotRepository;
        this.dispatchService = dispatchService;
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
        String orderNum = "ORD-" + System.currentTimeMillis() % 100000;
        DeliveryOrder order = new DeliveryOrder(
                orderNum,
                request.getCustomerName(),
                request.getDeliveryLatitude(),
                request.getDeliveryLongitude(),
                request.getPackageWeightKg(),
                request.getPriority(),
                request.getOriginDepotId()
        );

        DeliveryOrder saved = orderRepository.save(order);

        telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                "INFO", "Order Created",
                "New order #" + orderNum + " received for " + request.getCustomerName() + " (" + request.getPackageWeightKg() + "kg)",
                null, saved.getId()
        ));

        // Trigger instant dispatch
        dispatchService.dispatchPendingOrders();

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/simulate-batch")
    public ResponseEntity<List<DeliveryOrder>> simulateBatchOrders(@RequestParam(defaultValue = "5") int count) {
        List<Depot> depots = depotRepository.findAll();
        if (depots.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String[] customers = {
                "Oakridge Medical Clinic", "Highland Tech Labs", "Metro General Hospital",
                "Westwood Pharmacy", "Downtown Grocers", "Riverside Research",
                "Sunset Boulevard Cafe", "Horizon Biotech Hub"
        };

        String[] priorities = {"STANDARD", "EXPRESS", "MEDICAL"};

        List<DeliveryOrder> createdOrders = new ArrayList<>();

        for (int i = 0; i < Math.min(count, 15); i++) {
            Depot baseDepot = depots.get(random.nextInt(depots.size()));

            // Random drop within ~3.5km of base depot
            double latOffset = (random.nextDouble() - 0.5) * 0.05;
            double lonOffset = (random.nextDouble() - 0.5) * 0.05;
            double targetLat = baseDepot.getLatitude() + latOffset;
            double targetLon = baseDepot.getLongitude() + lonOffset;

            String customer = customers[random.nextInt(customers.length)];
            String priority = priorities[random.nextInt(priorities.length)];
            double weight = Math.round((0.5 + (random.nextDouble() * 4.5)) * 10.0) / 10.0;
            String orderNum = "ORD-" + (1000 + random.nextInt(9000));

            DeliveryOrder order = new DeliveryOrder(
                    orderNum, customer, targetLat, targetLon, weight, priority, baseDepot.getId()
            );

            createdOrders.add(orderRepository.save(order));
        }

        telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                "INFO", "Batch Orders Queued",
                "Generated " + createdOrders.size() + " simulation delivery orders.",
                null, null
        ));

        dispatchService.dispatchPendingOrders();

        return ResponseEntity.ok(createdOrders);
    }
}
