# AeroFleet 🚁⚡
### Autonomous Delivery Drone Fleet Simulation Platform

AeroFleet is an event-driven delivery drone simulation platform built with **Java Spring Boot**. It simulates autonomous drone operations across urban and suburban environments, handling dynamic package dispatching, realistic flight kinematics, battery discharge under payload and weather drag, geofenced No-Fly Zone (NFZ) avoidance, failsafe Return-To-Home (RTH) procedures, and real-time telemetry streaming over WebSockets to an interactive browser dashboard.

---

## 🌟 Key Features

1. **Simulation & Flight Kinematics Engine**:
   - Multi-phase 3D flight profiles: `TAKEOFF` $\to$ `EN_ROUTE_DELIVERY` $\to$ `DESCENDING_DELIVERY` $\to$ `DELIVERING` $\to$ `ASCENDING_DELIVERY` $\to$ `RETURNING` $\to$ `LANDING` $\to$ `CHARGING`.
   - Spherical distance and bearing math using the Haversine formula and waypoint progression.
   - Dynamic time-warp multiplier (1x, 2x, 5x, 10x) and simulation pause/resume.

2. **Energy & Battery Consumption Model**:
   - Dynamic power draw factoring avionics baseline, payload mass ratio ($W_{\text{payload}} / W_{\text{max}}$), vertical climb penalty, and environmental drag.
   - Automatic battery charging on depot pads at 3%/sec until full.
   - Low-battery failsafe Return-To-Home (RTH) triggering immediate diversion to the nearest depot.

3. **Geofencing & Airspace Protection**:
   - 3D No-Fly Zones (NFZs) around sensitive sites (hospitals, heliports, government facilities).
   - Real-time path intersection detection and automated detour waypoint deflection.

4. **Intelligent Dispatch Engine**:
   - Evaluates pending orders and pairs them with optimal drones based on payload capacity, proximity, battery state-of-charge, and range reachability.

5. **Real-Time Telemetry & Modern Web Dashboard**:
   - High-frequency STOMP over WebSocket streaming to a single-page Leaflet.js radar map.
   - Live drone cards with battery gauges, altitude, speed, payload indicators, manual recall, and fault injection buttons.
   - One-click batch delivery generator and interactive map click-to-dispatch.

---

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.3.5, Spring Data JPA, Spring WebSocket (STOMP/SockJS), Jakarta Validation
- **Database**: H2 In-Memory Database (with preloaded depots, drones, and airspace zones)
- **Frontend**: Single-Page Application (HTML5, Tailwind CSS, Leaflet.js, Lucide Icons, SockJS, StompJS)
- **Build & Runtime**: Maven Wrapper (`mvnw`), Java 17 / 21+

---

## 🚀 Quick Start

### 1. Build and Run the Application

Using the bundled Maven wrapper:

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Alternatively with system Maven:
```bash
mvn spring-boot:run
```

### 2. Open the Live Mission Control Center

Open your browser and navigate to:
```
http://localhost:8080
```

- **H2 Database Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:aerofleetdb`, User: `sa`, Password: empty)

---

## 📡 REST API Reference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/drones` | List all drones with current telemetry and status |
| `GET` | `/api/v1/drones/{id}` | Get specific drone details |
| `POST` | `/api/v1/drones/{id}/recall` | Trigger manual emergency Return-To-Home |
| `GET` | `/api/v1/orders` | List delivery orders (optional `?status=PENDING`) |
| `POST` | `/api/v1/orders` | Create a new delivery order |
| `POST` | `/api/v1/orders/simulate-batch` | Generate $N$ randomized delivery missions |
| `GET` | `/api/v1/depots` | List all base stations and charging pads |
| `GET` | `/api/v1/geofence` | List active No-Fly Zones (NFZs) |
| `GET` | `/api/v1/simulation/status` | Current clock, speed multiplier, and weather |
| `POST` | `/api/v1/simulation/control` | Control pause/play, speed (1x-10x), and weather |
| `POST` | `/api/v1/simulation/incident` | Inject fault (e.g. `BATTERY_DRAIN`, `MOTOR_FAULT`) |

---

## 🧪 Running Automated Tests

Run the test suite verifying kinematics, battery model, geofencing, and Spring context:

```bash
.\mvnw.cmd test
```
