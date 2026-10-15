package com.aerofleet.simulation;

import com.aerofleet.controller.TelemetryBroadcaster;
import com.aerofleet.domain.enums.DroneStatus;
import com.aerofleet.domain.enums.OrderStatus;
import com.aerofleet.domain.model.DeliveryOrder;
import com.aerofleet.domain.model.Depot;
import com.aerofleet.domain.model.Drone;
import com.aerofleet.domain.model.GeoCoordinate;
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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    // In-memory runtime cache for drones with active waypoints
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
        syncRuntimeDronesFromDb();
        scheduler.scheduleAtFixedRate(this::tick, 1000, tickIntervalMs, TimeUnit.MILLISECONDS);
        log.info("AeroFleet Simulation Engine started at {}ms tick interval", tickIntervalMs);
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

    /**
     * Core simulation tick loop running once per second.
     */
    public void tick() {
        try {
            if (!running) {
                broadcastSnapshot();
                return;
            }

            double deltaSeconds = 1.0 * speedMultiplier;
            simTimeElapsedSeconds += (long) deltaSeconds;

            // 1. Dispatch pending orders to eligible drones
            dispatchService.dispatchPendingOrders();

            // Refresh runtime map if new drones were assigned
            List<Drone> dbDrones = droneRepository.findAll();
            for (Drone dbDrone : dbDrones) {
                Drone runtimeDrone = runtimeDrones.get(dbDrone.getId());
                if (runtimeDrone == null) {
                    runtimeDrones.put(dbDrone.getId(), dbDrone);
                } else if (runtimeDrone.getAssignedOrderId() == null && dbDrone.getAssignedOrderId() != null) {
                    // Update runtime drone with route assigned by DispatchService
                    runtimeDrone.setAssignedOrderId(dbDrone.getAssignedOrderId());
                    runtimeDrone.setCurrentPayloadKg(dbDrone.getCurrentPayloadKg());
                    runtimeDrone.setStatus(dbDrone.getStatus());
                    runtimeDrone.setWaypoints(dbDrone.getWaypoints());
                }
            }

            // 2. Process each drone's flight kinematics & battery
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

        // Handle stationary states
        if (status == DroneStatus.IDLE || status == DroneStatus.MAINTENANCE) {
            return;
        }

        // Handle charging on depot pad
        if (status == DroneStatus.CHARGING) {
            double newBattery = batteryDrainModel.updateBattery(drone, deltaSeconds, 0.0);
            drone.setBatteryPercent(newBattery);
            if (newBattery >= 100.0) {
                drone.setStatus(DroneStatus.IDLE);
                telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                        "INFO", "Charging Complete", "Drone [" + drone.getCallsign() + "] is at 100% battery.",
                        drone.getId(), null
                ));
            }
            return;
        }

        // Active Flight States
        Waypoint targetWaypoint = drone.getCurrentWaypoint();
        if (targetWaypoint == null) {
            if (status == DroneStatus.LANDING || status == DroneStatus.RETURNING) {
                drone.setStatus(DroneStatus.CHARGING);
                drone.setCurrentSpeedMps(0.0);
                drone.setCurrentAltitudeMeters(0.0);
            }
            return;
        }

        GeoCoordinate currentCoord = drone.getCurrentCoordinate();
        GeoCoordinate targetCoord = targetWaypoint.getCoordinate();

        double distanceToTarget = currentCoord.distanceTo(targetCoord);
        double targetSpeed = targetWaypoint.getTargetSpeedMps();
        double stepDistance = targetSpeed * deltaSeconds;

        double altitudeChange = 0.0;

        // Waypoint arrival check
        if (distanceToTarget <= Math.max(stepDistance, 6.0) || targetSpeed == 0.0) {
            handleWaypointArrival(drone, targetWaypoint);
        } else {
            // Move drone towards waypoint
            GeoCoordinate nextCoord = currentCoord.moveTowards(targetCoord, stepDistance);
            double heading = currentCoord.bearingTo(targetCoord);

            altitudeChange = nextCoord.getAltitudeMeters() - drone.getCurrentAltitudeMeters();

            drone.setCurrentCoordinate(nextCoord);
            drone.setHeadingDegrees(heading);
            drone.setCurrentSpeedMps(targetSpeed);
            drone.setStatus(targetWaypoint.getExpectedStatus());
            drone.setTotalDistanceTraveledMeters(drone.getTotalDistanceTraveledMeters() + stepDistance);
            drone.setTotalFlightTimeSeconds(drone.getTotalFlightTimeSeconds() + (long) deltaSeconds);
        }

        // Update battery drain under flight & weather
        double newBattery = batteryDrainModel.updateBattery(drone, deltaSeconds, altitudeChange);
        drone.setBatteryPercent(newBattery);

        // Check Low-Battery Return-To-Home Failsafe
        if (newBattery <= minReturnBatteryPercent &&
                status != DroneStatus.RETURNING &&
                status != DroneStatus.EMERGENCY_RTH &&
                status != DroneStatus.LANDING &&
                status != DroneStatus.CHARGING) {
            triggerEmergencyRth(drone, "Low battery warning: " + String.format("%.1f%%", newBattery));
        }
    }

    private void handleWaypointArrival(Drone drone, Waypoint waypoint) {
        DroneStatus expected = waypoint.getExpectedStatus();

        if (expected == DroneStatus.DELIVERING) {
            // Package delivered to customer!
            if (drone.getAssignedOrderId() != null) {
                orderRepository.findById(drone.getAssignedOrderId()).ifPresent(order -> {
                    order.setStatus(OrderStatus.DELIVERED);
                    order.setDeliveredAt(LocalDateTime.now());
                    orderRepository.save(order);

                    telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                            "SUCCESS", "Package Delivered",
                            "Order #" + order.getOrderNumber() + " delivered successfully by " + drone.getCallsign(),
                            drone.getId(), order.getId()
                    ));
                    telemetryBroadcaster.broadcastOrderUpdate(order);
                });
            }
            drone.setCurrentPayloadKg(0.0);
            drone.setSuccessfulDeliveriesCount(drone.getSuccessfulDeliveriesCount() + 1);
        } else if (expected == DroneStatus.LANDING) {
            // Landed back on pad at depot
            drone.setStatus(DroneStatus.CHARGING);
            drone.setCurrentSpeedMps(0.0);
            drone.setCurrentAltitudeMeters(0.0);
            drone.setAssignedOrderId(null);
            drone.clearFlightPlan();

            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    "INFO", "Drone Docked",
                    "Drone [" + drone.getCallsign() + "] landed on charging pad.",
                    drone.getId(), null
            ));
            return;
        }

        drone.advanceWaypoint();
    }

    public void triggerEmergencyRth(Drone drone, String reason) {
        log.warn("Triggering Emergency RTH for Drone [{}] due to: {}", drone.getCallsign(), reason);

        // Find nearest depot
        List<Depot> depots = depotRepository.findAll();
        Depot nearestDepot = depots.stream()
                .min(Comparator.comparingDouble(d -> d.toCoordinate().distanceTo(drone.getCurrentCoordinate())))
                .orElse(null);

        if (nearestDepot != null) {
            List<Waypoint> rthRoute = routingService.buildEmergencyRthRoute(drone, nearestDepot);
            drone.setWaypoints(rthRoute);
            drone.setStatus(DroneStatus.EMERGENCY_RTH);
            drone.setCurrentDepotId(nearestDepot.getId());

            telemetryBroadcaster.broadcastAlert(new AlertEventDTO(
                    "DANGER", "Emergency RTH Initiated",
                    drone.getCallsign() + " aborting mission: " + reason,
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
            triggerEmergencyRth(drone, "Injected motor telemetry warning");
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
            pkt.setLatitude(drone.getCurrentLatitude());
            pkt.setLongitude(drone.getCurrentLongitude());
            pkt.setAltitudeMeters(drone.getCurrentAltitudeMeters());
            pkt.setSpeedMps(drone.getCurrentSpeedMps());
            pkt.setHeadingDegrees(drone.getHeadingDegrees());
            pkt.setBatteryPercent(drone.getBatteryPercent());
            pkt.setPayloadKg(drone.getCurrentPayloadKg());
            pkt.setStatus(drone.getStatus());
            pkt.setAssignedOrderId(drone.getAssignedOrderId());
            pkt.setDepotId(drone.getCurrentDepotId());

            Waypoint wp = drone.getCurrentWaypoint();
            if (wp != null) {
                pkt.setTargetLat(wp.getCoordinate().getLatitude());
                pkt.setTargetLon(wp.getCoordinate().getLongitude());
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
