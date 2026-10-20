# AeroFleet — Code-Level & Implementation Documentation

This document provides a class-by-class, method-by-method, and pattern-by-pattern breakdown of the **AeroFleet Codebase** for college project presentation, code review, and viva evaluation.

---

## 📋 Table of Contents
1. [Codebase Organization & Package Taxonomy](#1-codebase-organization--package-taxonomy)
2. [Domain Layer (`com.aerofleet.domain`)](#2-domain-layer)
   - [2.1 Entities & Models](#21-entities--models)
   - [2.2 Enums](#22-enums)
3. [Service & Simulation Layer (`com.aerofleet.service` & `simulation`)](#3-service--simulation-layer)
   - [3.1 `SimulationEngine.java`](#31-simulationenginejava)
   - [3.2 `RoutingService.java`](#32-routingservicejava)
   - [3.3 `GeofenceService.java`](#33-geofenceservicejava)
   - [3.4 `DispatchService.java`](#34-dispatchservicejava)
   - [3.5 `BatteryDrainModel.java`](#35-batterydrainmodeljava)
   - [3.6 `WeatherService.java`](#36-weatherservicejava)
4. [Controller & WebSocket Layer (`com.aerofleet.controller`)](#4-controller--websocket-layer)
5. [Frontend Engine (`static/js/app.js`)](#5-frontend-engine)
6. [OOP Principles & Design Patterns Used](#6-oop-principles--design-patterns-used)
7. [Viva & Project Defense Q&A](#7-viva--project-defense-qa)

---

## 1. Codebase Organization & Package Taxonomy

```
com.aerofleet
├── AeroFleetApplication.java          # Spring Boot main entry point & @EnableScheduling
├── config
│   └── WebSocketConfig.java          # STOMP message broker & SockJS endpoint registration
├── controller
│   ├── DepotController.java          # REST API for ground charging hubs
│   ├── DroneController.java          # REST API for drone telemetry & operator recall
│   ├── GeofenceController.java       # REST API for dynamic No-Fly Zone CRUD & toggle
│   ├── OrderController.java          # REST API for customer orders & batch mission generation
│   ├── SimulationController.java     # REST API for simulation speed, clock, and weather controls
│   └── TelemetryBroadcaster.java     # STOMP WebSocket publisher (/topic/telemetry, /topic/alerts)
├── domain
│   ├── enums
│   │   ├── DroneModelType.java       # Specs: speed, payload limit, battery capacity
│   │   ├── DroneStatus.java          # FSM states: IDLE, CHARGING, EN_ROUTE_DELIVERY, etc.
│   │   ├── OrderStatus.java          # PENDING, IN_TRANSIT, DELIVERED, FAILED
│   │   └── WeatherCondition.java     # CLEAR, WINDY, RAIN, STORM
│   └── model
│       ├── DeliveryOrder.java        # JPA entity for customer delivery missions
│       ├── Depot.java                # JPA entity for ground charging base stations
│       ├── Drone.java                # JPA entity & in-memory flight agent
│       ├── Location.java             # Immutable 2D Cartesian point (x, y) with vector methods
│       ├── NoFlyZone.java            # JPA entity representing circular hazard zone
│       └── Waypoint.java             # Sequential 2D navigation node with target speed & status
├── dto
│   ├── AlertEventDTO.java            # WebSocket payload for system and danger alerts
│   ├── CreateOrderRequest.java       # Bean validation DTO for new orders
│   ├── CreateZoneRequest.java        # Bean validation DTO for new restricted zones
│   ├── FleetSnapshotDTO.java         # Global telemetry broadcast packet
│   ├── SimulationControlDTO.java     # DTO for simulation play/pause, speed, and weather
│   └── TelemetryPacketDTO.java       # Per-drone telemetry packet with remaining waypoints
├── repository
│   ├── DeliveryOrderRepository.java  # Spring Data JPA for orders
│   ├── DepotRepository.java          # Spring Data JPA for depots
│   ├── DroneRepository.java          # Spring Data JPA for drones
│   └── NoFlyZoneRepository.java      # Spring Data JPA for No-Fly Zones
├── service
│   ├── DispatchService.java          # Cost-function based order-to-drone matching algorithm
│   ├── GeofenceService.java          # Vector geometry & bypass calculation
│   ├── RoutingService.java           # Multi-zone recursive waypoint generation
│   └── WeatherService.java           # Atmospheric state & wind vector calculations
└── simulation
    ├── BatteryDrainModel.java        # Physics-based energy discharge and recharge
    └── SimulationEngine.java         # Master discrete-time flight execution loop
```

---

## 2. Domain Layer

### 2.1 Entities & Models

#### `Location.java`
Represents an immutable 2D Cartesian point $(x, y)$.
- `distanceTo(Location other)`: Implements Pythagorean formula:
  $$\sqrt{(x_2 - x_1)^2 + (y_2 - y_1)^2}$$
- `bearingTo(Location other)`: Computes compass heading in degrees using `Math.atan2(dy, dx)`.
- `moveTowards(Location target, double stepDistance)`: Computes unit vector $\hat{u} = \frac{\Delta\vec{r}}{\|\Delta\vec{r}\|}$ and translates by $\text{stepDistance}$.

#### `Drone.java`
Core simulation entity tracking active position, kinematics, battery, assigned order, and current waypoint queue.
- `advanceWaypoint()`: Increments `currentWaypointIndex` and updates expected status.
- `clearFlightPlan()`: Resets waypoint queue upon landing or emergency abort.

#### `NoFlyZone.java`
Represents a circular restricted zone with center $(c_x, c_y)$ and radius $R$.
- `violatesZone(Location loc)`: Evaluates $(x - c_x)^2 + (y - c_y)^2 \le R^2$.
- `intersectsPath(Location p1, Location p2)`: Implements exact Euclidean closest-point projection of line segment $P_1 P_2$ onto $C$. Returns `true` if closest distance $\le R$.

#### `Waypoint.java`
A target waypoint containing `Location location`, `double targetSpeed`, `DroneStatus expectedStatus`, and human-readable `String description`.

---

### 2.2 Enums

#### `DroneModelType.java`
Defines drone hardware specifications:
- `QUADCOPTER_STANDARD`: Max speed $35.0\text{ u/s}$, max payload $3.0\text{ kg}$, battery capacity $5000\text{ mAh}$.
- `HEXACOPTER_HEAVY`: Max speed $25.0\text{ u/s}$, max payload $8.0\text{ kg}$, battery capacity $8000\text{ mAh}$.
- `VTOL_FIXED_WING`: Max speed $55.0\text{ u/s}$, max payload $4.5\text{ kg}$, battery capacity $10000\text{ mAh}$.

#### `DroneStatus.java`
Finite State Machine states: `IDLE`, `CHARGING`, `ASSIGNED`, `EN_ROUTE_DELIVERY`, `DELIVERING`, `RETURNING`, `LANDING`, `EMERGENCY_RTH`, `MAINTENANCE`.

---

## 3. Service & Simulation Layer

### 3.1 `SimulationEngine.java`
The heartbeat of the platform running on a discrete 1-second scheduled executor loop:
- `processDroneTick(Drone drone, double deltaSeconds)`:
  - **Auto-dock charging**: Detects if drone is stationary at a depot charging pad (`isStationaryAtDepot`) with `< 100%` battery; automatically transitions `status` to `CHARGING`.
  - **Kinematics step**: Translates drone along vector to `currentWaypoint` taking into account directional wind vector from `WeatherService`.
  - **Airspace incursion detection**: Checks `geofenceService.isPointInRestrictedAirspace(nextLoc)` on each step; emits alert upon unauthorized boundary entry/exit.
  - **Battery model invocation**: Drains battery or recharges on pad.
  - **Failsafe low-battery check**: Triggers emergency RTH if battery drops below $20.0\%$.
- `rerouteActiveDronesForZoneUpdate()`: Scans in-flight drones when airspace changes and recalculates waypoints around new zones.
- `triggerEmergencyRth(Drone drone, String reason)`: Diverts drone to closest ground hub with safety detour waypoints.

### 3.2 `RoutingService.java`
- `planClearPath(Location start, Location end, int depth)`: Recursive function finding the closest intersecting zone, deriving detour coordinates via `GeofenceService`, and recursively evaluating each leg until clear.
- `buildDeliveryRoute(Drone, Depot, DeliveryOrder)`: Constructs the complete 6-phase mission route:
  1. Depot $\to$ Outbound Detours $\to$ Customer target (`EN_ROUTE_DELIVERY`).
  2. Drop package at target coordinates with zero speed (`DELIVERING`).
  3. Customer target $\to$ Return Detours $\to$ Depot (`RETURNING`).
  4. Descent touchdown on pad (`LANDING`).
- `replanInFlightDrone(Drone, DeliveryOrder, Depot)`: Dynamically rebuilds remaining waypoints from the drone's instantaneous $(x, y)$ coordinate.

### 3.3 `GeofenceService.java`
- `getClosestIntersectedZone(Location start, Location end)`: Filters active zones intersecting segment and returns the one closest to `start`.
- `calculateBypassWaypoint(Location start, Location end, NoFlyZone zone)`:
  Computes both perpendicular normal vectors $+N$ and $-N$, expands by $1.35\times$ radius buffer, clamps to canvas margins, and selects the direction that minimizes total flight distance.

### 3.4 `DispatchService.java`
Autonomous mission assignment:
- `findOptimalDrone(order, candidateDrones, depots)`:
  - Filters drones: status is `IDLE` or (`CHARGING` with $\ge 70\%$ battery).
  - Filters capacity: `drone.maxPayload >= order.weight`.
  - Filters battery sufficiency: `battery >= estimatedRoundtripDrain + 15% safety buffer`.
  - Cost function minimization:
    $$\text{Cost} = \text{Distance}(\text{Depot}, \text{Customer}) - (\text{BatteryPercent} \times 2.0)$$
    Favors closer depots and drones with fuller batteries.

### 3.5 `BatteryDrainModel.java`
- Calculates discharge in flight factoring base avionics ($0.4\%/\text{s}$), cargo payload weight ratio ($0.3\%/\text{s} \times \frac{w}{w_{\text{max}}}$), and weather drag multiplier.
- Calculates recharge rate on pad ($3.0\%/\text{s}$).

### 3.6 `WeatherService.java`
Maintains `WeatherCondition` (`CLEAR`, `WINDY`, `RAIN`, `STORM`), `windSpeedMps`, and `windDirectionDeg`. Calculates drag factor $\mu_{\text{drag}} = \text{base} + \frac{w_s}{20.0}$.

---

## 4. Controller & WebSocket Layer

- **`GeofenceController.java`**: Implements REST operations for zones (`POST`, `DELETE`, `PUT /toggle`). Crucially calls `simulationEngine.rerouteActiveDronesForZoneUpdate()` upon every mutation to ensure real-time airspace compliance.
- **`DroneController.java`**: Provides drone inspection and manual operator emergency recall.
- **`OrderController.java`**: Handles new order submissions and batch generation (`/simulate-batch`).
- **`TelemetryBroadcaster.java`**: Encapsulates `SimpMessagingTemplate` to push `FleetSnapshotDTO` and `AlertEventDTO` over STOMP topics `/topic/telemetry` and `/topic/alerts`.

---

## 5. Frontend Engine (`static/js/app.js`)

- **Canvas Rendering Loop**: 60 FPS `requestAnimationFrame` loop on `<canvas id="sim-canvas">` ($800 \times 600$).
- **Linear Interpolation (LERP)**: Smooths drone movement across the discrete 1-second server snapshots:
  $$x_{\text{render}} = x_{\text{prev}} + (x_{\text{target}} - x_{\text{prev}}) \cdot \text{fraction}$$
- **Dynamic Rotor Animation**: Rotates quadcopter propeller graphics based on `Date.now() / 35`.
- **Full Flight Path Polyline**: Traverses `drone.remainingWaypoints` to draw dashed corridors and detour pins bending around hazard zones.
- **Interactive Modals**: Add Zone modal, Remove Zone modal, Environment modal, and Dispatch Order modal.

---

## 6. OOP Principles & Design Patterns Used

| Principle / Pattern | Where Used | Explanation |
|---|---|---|
| **Encapsulation** | `Drone`, `Location`, `NoFlyZone` | Private fields with clean getters/setters, state changes protected by domain methods (`advanceWaypoint`). |
| **Polymorphism** | `DroneModelType` | Each drone model type encapsulates distinct speed, payload, and power profiles accessed via uniform interface. |
| **Separation of Concerns** | Layers | Physics/Kinematics (`SimulationEngine`), Route Calculation (`RoutingService`), and Persistence (`Repositories`) are decoupled. |
| **Observer Pattern** | STOMP WebSockets | `TelemetryBroadcaster` notifies subscribed browser clients whenever telemetry updates or alerts occur. |
| **Strategy Pattern** | Battery & Routing | `BatteryDrainModel` and `RoutingService` can be swapped or modified without altering the simulation tick loop. |
| **State Pattern** | `DroneStatus` | Drone behavior in `processDroneTick` strictly depends on its finite operational state. |

---

## 7. Viva & Project Defense Q&A

### Q1: Why did you choose a 2D Cartesian grid instead of GPS/GIS lat-long coordinates?
**Answer**:
> *"For a college-level computer science miniproject, standard 2D Cartesian geometry ($800 \times 600$) demonstrates clean, pure mathematical modeling (Euclidean distance, vector translations, trigonometry) without requiring external, heavyweight GIS dependencies or internet mapping APIs. It allows examiners to clearly verify every kinematic formula and collision detection algorithm directly from first principles."*

### Q2: How does the drone detect that it is intersecting a No-Fly Zone?
**Answer**:
> *"Instead of relying on inaccurate discrete point sampling, we implemented exact Euclidean point-to-segment projection geometry in `NoFlyZone.java`. We project the circle's center $C$ onto the segment $\vec{AB}$ using the dot product formula, clamp the scalar $t$ to $[0, 1]$ to stay within the segment endpoints, and evaluate whether the squared distance to the closest point is $\le R^2$. This runs in $O(1)$ constant time with zero false negatives."*

### Q3: How do newly added No-Fly Zones affect drones already in flight?
**Answer**:
> *"Whenever a zone is added, deleted, or toggled in `GeofenceController`, the controller triggers `simulationEngine.rerouteActiveDronesForZoneUpdate()`. This scans all active in-flight drones (`EN_ROUTE_DELIVERY`, `RETURNING`). If a drone's remaining flight corridor intersects the new zone, `RoutingService.replanInFlightDrone()` recalculates its waypoints starting from its current instantaneous coordinates, safely curving around the new obstacle."*

### Q4: How does drone charging work?
**Answer**:
> *"When a drone arrives at a depot charging pad or when its battery is drained while idle at a depot, `SimulationEngine` detects that it is stationary at a hub. If its battery is $< 100\%$, it automatically transitions to `CHARGING`. The `BatteryDrainModel` increases charge by $3.0\%\text{ per second}$. Once the battery reaches $100.0\%$, the drone transitions back to `IDLE` and logs an alert, making it eligible for new delivery missions."*

### Q5: How does weather affect drone flight?
**Answer**:
> *"Weather affects two independent flight systems: ground speed and energy consumption. Using vector trigonometry, we determine the angle between the drone's heading and wind travel direction. A tailwind increases ground speed up to $+7.5\text{ u/s}$, while a headwind slows the drone down. Additionally, atmospheric drag scales battery discharge up to $2.5\times$ during severe storms."*
