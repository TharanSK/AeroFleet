package com.aerofleet.simulation;

import com.aerofleet.controller.TelemetryBroadcaster;
import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.enums.OrderStatus;
import com.aerofleet.domain.model.DeliveryOrder;
import com.aerofleet.domain.model.Depot;
import com.aerofleet.domain.model.Drone;
import com.aerofleet.domain.model.Location;
import com.aerofleet.domain.model.Waypoint;
import com.aerofleet.dto.AlertEventDTO;
import com.aerofleet.dto.FleetSnapshotDTO;
import com.aerofleet.dto.TelemetryPacketDTO;
import com.aerofleet.repository.DeliveryOrderRepository;
import com.aerofleet.repository.DepotRepository;
import com.aerofleet.repository.DroneRepository;
import com.aerofleet.service.DispatchService;
import com.aerofleet.service.RoutingService;
import com.aerofleet.service.WeatherService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Core simulation tick engine running the 2D grid discrete loop.
 * Demonstrates Multithreading (ScheduledExecutorService) and Concurrent Collections.
 */
@Service
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    private final DroneRepository droneRepository;
    private final DeliveryOrderRepository orderRepository;
    private final DepotRepository depotRepository;
    private final DispatchService dispatchService;
    private final RoutingService routingService;
    private final BatteryDrainModel batteryDrainModel;
    private final WeatherService weatherService;
    private final TelemetryBroadcaster telemetryBroadcaster;

    // Thread-safe in-memory cache for live drones with runtime flight plans
    private final ConcurrentHashMap<Long, Drone> runtimeDrones = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running = true;
    private volatile double speedMultiplier = 1.0;
    private volatile long simTimeElapsedSeconds = 0;

    @Value("${aerofleet.simulation.tick-interval-ms:1000}")
    private long tickIntervalMs;

    @Value("${aerofleet.simulation.min-return-battery-percent:20.0}")
    private double minReturnBatteryPercent;

    public SimulationEngine(
            DroneRepository droneRepository,
            DeliveryOrderRepository orderRepository,
            DepotRepository depotRepository,
            DispatchService dispatchService,
            RoutingService routingService,
            BatteryDrainModel batteryDrainModel,
            WeatherService weatherService,
            TelemetryBroadcaster telemetryBroadcaster) {
        this.droneRepository = droneRepository;
        this.orderRepository = orderRepository;
        this.depotRepository = depotRepository;
        this.dispatchService = dispatchService;
        this.routingService = routingService;
        this.batteryDrainModel = batteryDrainModel;
        this.weatherService = weatherService;
        this.telemetryBroadcaster = telemetryBroadcaster;
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::tick, 1000, tickIntervalMs, TimeUnit.MILLISECONDS);
        log.info("AeroFleet 2D Grid Simulation Engine scheduled at {}ms tick interval", tickIntervalMs);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncRuntimeDronesFromDb();
        log.info("AeroFleet Engine ready: loaded {} drones into runtime memory", runtimeDrones.size());
        triggerDispatch();
    }

    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
    }

    public synchronized void syncRuntimeDronesFromDb() {
        List<Drone> dbDrones = droneRepository.findAll();
        for (Drone d : dbDrones) {
            runtimeDrones.putIfAbsent(d.getId(), d);
        }
    }

    public synchronized void triggerDispatch() {
        if (runtimeDrones.isEmpty()) {
            syncRuntimeDronesFromDb();
        }
        dispatchService.dispatchPendingOrders(runtimeDrones.values());
    }

    public Collection<Drone> getRuntimeDrones() {
        if (runtimeDrones.isEmpty()) {
            syncRuntimeDronesFromDb();
        }
        return runtimeDrones.values();
    }

    /**
     * Fixed-rate discrete tick loop.
     */
    public void tick() {
        try {
            if (!running) {
                broadcastSnapshot();
                return;
            }

            if (runtimeDrones.isEmpty()) {
                syncRuntimeDronesFromDb();
            }

            double deltaSeconds = 1.0 * speedMultiplier;
            simTimeElapsedSeconds += (long) deltaSeconds;

            // 1. Dispatch pending orders directly to in-memory runtime drones
            dispatchService.dispatchPendingOrders(runtimeDrones.values());

            // 2. Advance 2D coordinates & battery for each drone
            for (Drone drone : runtimeDrones.values()) {
                processDroneTick(drone, deltaSeconds);
                droneRepository.save(drone);
            }

            // 3. Broadcast real-time snapshot over STOMP WebSocket
            broadcastSnapshot();

        } catch (Exception e) {
            log.error("Error during simulation tick", e);
        }
    }

    private void processDroneTick(Drone drone, double deltaSeconds) {
        DroneStatus status = drone.getStatus();

        // Stationary state
        if (status == DroneStatus.IDLE || status == DroneStatus.MAINTENANCE) {
            return;
        }

        // Charging on pad at depot
        if (status == DroneStatus.CHARGING) {
            double newBattery = batteryDrainModel.updateBattery(drone, deltaSeconds);
            drone.setBatteryPercent(newBattery);
            if (newBattery >= 100.0) {
                drone.setStatus(DroneStatus.IDLE);
                telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                        "INFO", "Charging Complete", "Drone [" + drone.getCallsign() + "] charged to 100%.",
                        drone.getId(), null
                ));
            }
            return;
        }

        // Active Flight State
        Waypoint targetWaypoint = drone.getCurrentWaypoint();
        if (targetWaypoint == null) {
            if (status == DroneStatus.LANDING || status == DroneStatus.RETURNING) {
                drone.setStatus(DroneStatus.CHARGING);
                drone.setSpeed(0.0);
            } else {
                drone.setStatus(DroneStatus.IDLE);
                drone.setSpeed(0.0);
            }
            return;
        }

        Location currentLoc = drone.getLocation();
        Location targetLoc = targetWaypoint.getLocation();

        double distanceToTarget = currentLoc.distanceTo(targetLoc);
        double targetSpeed = targetWaypoint.getTargetSpeed();
        double stepDistance = targetSpeed * deltaSeconds;

        // Waypoint arrival check
        if (distanceToTarget <= Math.max(stepDistance, 8.0) || targetSpeed == 0.0) {
            handleWaypointArrival(drone, targetWaypoint);
        } else {
            // Move along 2D vector towards target
            Location nextLoc = currentLoc.moveTowards(targetLoc, stepDistance);
            double heading = currentLoc.bearingTo(targetLoc);

            drone.setLocation(nextLoc);
            drone.setHeadingDegrees(heading);
            drone.setSpeed(targetSpeed);
            drone.setStatus(targetWaypoint.getExpectedStatus());
            drone.setTotalDistanceTraveled(drone.getTotalDistanceTraveled() + stepDistance);
            drone.setTotalFlightTimeSeconds(drone.getTotalFlightTimeSeconds() + (long) deltaSeconds);
        }

        // Update battery percentage
        double newBattery = batteryDrainModel.updateBattery(drone, deltaSeconds);
        drone.setBatteryPercent(newBattery);

        // Failsafe: Emergency Return-To-Home
        if (newBattery <= minReturnBatteryPercent &&
                status != DroneStatus.RETURNING &&
                status != DroneStatus.EMERGENCY_RTH &&
                status != DroneStatus.LANDING &&
                status != DroneStatus.CHARGING) {
            triggerEmergencyRth(drone, "Low battery failsafe: " + String.format("%.1f%%", newBattery));
        }
    }

    private void handleWaypointArrival(Drone drone, Waypoint waypoint) {
        DroneStatus expected = waypoint.getExpectedStatus();

        if (expected == DroneStatus.DELIVERING) {
            // Package delivered!
            if (drone.getAssignedOrderId() != null) {
                orderRepository.findById(drone.getAssignedOrderId()).ifPresent(order -> {
                    order.setStatus(OrderStatus.DELIVERED);
                    order.setDeliveredAt(LocalDateTime.now());
                    orderRepository.save(order);

                    telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                            "SUCCESS", "Delivered",
                            "Order #" + order.getOrderNumber() + " delivered by " + drone.getCallsign(),
                            drone.getId(), order.getId()
                    ));
                    telemetryBroadcaster.broadcastOrderUpdate(order);
                });
            }
            drone.setCurrentPayloadKg(0.0);
            drone.setSuccessfulDeliveriesCount(drone.getSuccessfulDeliveriesCount() + 1);
        } else if (expected == DroneStatus.LANDING) {
            // Landed back at depot
            drone.setStatus(DroneStatus.CHARGING);
            drone.setSpeed(0.0);
            drone.setAssignedOrderId(null);
            drone.clearFlightPlan();

            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    "INFO", "Landed at Hub",
                    "Drone [" + drone.getCallsign() + "] docked on charging pad.",
                    drone.getId(), null
            ));
            return;
        }

        drone.advanceWaypoint();
    }

    public void triggerEmergencyRth(Drone drone, String reason) {
        log.warn("Emergency RTH triggered for Drone [{}] due to: {}", drone.getCallsign(), reason);

        // Find nearest depot on 2D grid
        List<Depot> depots = depotRepository.findAll();
        Depot nearestDepot = depots.stream()
                .min(Comparator.comparingDouble(d -> d.getLocation().distanceTo(drone.getLocation())))
                .orElse(null);

        if (nearestDepot != null) {
            List<Waypoint> rthRoute = routingService.buildEmergencyRthRoute(drone, nearestDepot);
            drone.setWaypoints(rthRoute);
            drone.setStatus(DroneStatus.EMERGENCY_RTH);
            drone.setCurrentDepotId(nearestDepot.getId());

            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    "DANGER", "Emergency RTH",
                    drone.getCallsign() + " aborting: " + reason,
                    drone.getId(), drone.getAssignedOrderId()
            ));

            if (drone.getAssignedOrderId() != null) {
                orderRepository.findById(drone.getAssignedOrderId()).ifPresent(order -> {
                    order.setStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
                    telemetryBroadcaster.broadcastOrderUpdate(order);
                });
            }
        }
    }

    public void injectIncident(Long droneId, String type, double magnitude) {
        Drone drone = runtimeDrones.get(droneId);
        if (drone == null) return;

        if ("BATTERY_DRAIN".equalsIgnoreCase(type)) {
            double current = drone.getBatteryPercent();
            drone.setBatteryPercent(Math.max(5.0, current - magnitude));
            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    "WARN", "Incident Injected",
                    "Forced battery drop by " + magnitude + "% on " + drone.getCallsign(),
                    drone.getId(), null
            ));
        } else if ("MOTOR_FAULT".equalsIgnoreCase(type)) {
            triggerEmergencyRth(drone, "Simulated motor fault");
        }
    }

    private void broadcastSnapshot() {
        FleetSnapshotDTO snapshot = new FleetSnapshotDTO();
        snapshot.setSimulationRunning(this.running);
        snapshot.setSpeedMultiplier(this.speedMultiplier);
        snapshot.setSimTimeElapsedSeconds(this.simTimeElapsedSeconds);
        snapshot.setWeatherCondition(weatherService.getCondition());
        snapshot.setWindSpeedMps(weatherService.getWindSpeedMps());
        snapshot.setWindDirectionDeg(weatherService.getWindDirectionDeg());

        int activeDeliveries = 0;
        int chargingDrones = 0;
        int idleDrones = 0;

        for (Drone drone : runtimeDrones.values()) {
            TelemetryPacketDTO pkt = new TelemetryPacketDTO();
            pkt.setDroneId(drone.getId());
            pkt.setCallsign(drone.getCallsign());
            pkt.setModelType(drone.getModelType());
            pkt.setX(drone.getX());
            pkt.setY(drone.getY());
            pkt.setSpeed(drone.getSpeed());
            pkt.setHeadingDegrees(drone.getHeadingDegrees());
            pkt.setBatteryPercent(drone.getBatteryPercent());
            pkt.setPayloadKg(drone.getCurrentPayloadKg());
            pkt.setStatus(drone.getStatus());
            pkt.setAssignedOrderId(drone.getAssignedOrderId());
            pkt.setDepotId(drone.getCurrentDepotId());

            Waypoint wp = drone.getCurrentWaypoint();
            if (wp != null) {
                pkt.setTargetX(wp.getLocation().getX());
                pkt.setTargetY(wp.getLocation().getY());
            }
            pkt.setWaypointIndex(drone.getCurrentWaypointIndex());
            pkt.setTotalWaypoints(drone.getWaypoints() != null ? drone.getWaypoints().size() : 0);

            snapshot.getDrones().add(pkt);

            if (drone.getStatus() == DroneStatus.CHARGING) {
                chargingDrones++;
            } else if (drone.getStatus() == DroneStatus.IDLE) {
                idleDrones++;
            } else {
                activeDeliveries++;
            }
        }

        snapshot.setTotalDrones(runtimeDrones.size());
        snapshot.setActiveDeliveries(activeDeliveries);
        snapshot.setChargingDrones(chargingDrones);
        snapshot.setIdleDrones(idleDrones);
        snapshot.setPendingOrders((int) orderRepository.countByStatus(OrderStatus.PENDING));
        snapshot.setCompletedOrders((int) orderRepository.countByStatus(OrderStatus.DELIVERED));

        telemetryBroadcaster.broadcastFleetSnapshot(snapshot);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = Math.max(0.2, Math.min(20.0, speedMultiplier));
    }

    public long getSimTimeElapsedSeconds() {
        return simTimeElapsedSeconds;
    }

    public void resetSimulation() {
        this.simTimeElapsedSeconds = 0;
        syncRuntimeDronesFromDb();
    }
}
