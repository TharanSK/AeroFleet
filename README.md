# AeroFleet 🚁⚡
### Autonomous 2D Drone Delivery Fleet Simulation Platform
*A clean, educational, and viva-ready autonomous drone fleet simulator engineered for College Computer Science Miniprojects.*

---

## 📖 Master Documentation Index

| Document | Description |
|---|---|
| 📑 **[API Documentation](docs/API_DOCUMENTATION.md)** | Full REST endpoints, request/response schemas, and WebSocket (STOMP) telemetry topics. |
| 📐 **[Design Documentation](docs/DESIGN_DOCUMENTATION.md)** | System architecture, Cartesian physics, kinematics math, battery drain models, and state machines. |
| 💻 **[Code-Level Documentation](docs/CODE_LEVEL_DOCUMENTATION.md)** | Package breakdown, class & method taxonomy, OOP patterns used, and Top 5 Viva Defense Q&As. |

---

## 🌟 Project Overview

**AeroFleet** simulates autonomous drone operations across an $800 \times 600$ 2D Cartesian grid. It demonstrates clean object-oriented architecture and fundamental physics modeling without requiring external, heavyweight GIS dependencies or internet mapping APIs.

### Key Capabilities:
1. **Kinematics & Vector Flight Engine**:
   - Discrete 1 Hz tick loop with multi-phase missions: `IDLE` $\to$ `CHARGING` $\to$ `EN_ROUTE_DELIVERY` $\to$ `DELIVERING` $\to$ `RETURNING` $\to$ `LANDING`.
   - Trigonometric heading ($\text{atan2}$) and step translations.
   - Dynamic simulation speed control ($0.2\times$ to $20\times$) and pause/resume.
2. **Dynamic Airspace Management (No-Fly Zones)**:
   - Operators can dynamically add, delete, or toggle circular restricted hazard zones.
   - **Exact Euclidean point-to-segment geometry**: Eliminates false negatives and detects path incursions in $O(1)$ time.
   - **In-flight replanning**: Active drones immediately curve around newly placed or activated zones.
   - **Recursive detour routing**: Routes safely around multiple overlapping obstacles.
3. **Depot Auto-Dock Charging**:
   - Drones stationary at ground base stations with $< 100\%$ battery automatically dock and recharge at $3.0\%/\text{sec}$ until full.
4. **Atmospheric Weather & Wind Modeling**:
   - Interactive weather presets (`CLEAR`, `WINDY`, `RAIN`, `STORM`), variable wind speed ($0$–$25\text{ m/s}$), and $360^\circ$ directional compass heading.
   - Tailwind assists forward ground speed; headwind introduces aerodynamic resistance.
   - Battery consumption scales dynamically with atmospheric drag.
5. **Intelligent Dispatch Engine**:
   - Cost-function optimization pairs pending delivery orders with the closest available drone capable of carrying the payload with sufficient battery buffer.
6. **Real-Time Telemetry & Radar Canvas**:
   - High-performance HTML5 Canvas radar rendering at 60 FPS with linear interpolation (LERP) and animated quadcopter rotors.
   - Full planned flight corridor polylines displaying real-time detour bends around hazard zones.
   - Instant notifications over WebSockets (STOMP).

---

## 🛠️ Technology Stack

- **Backend**: Java 17 / 21+, Spring Boot 3.3.5, Spring Data JPA, Spring WebSocket (STOMP / SockJS), Jakarta Validation
- **Database**: H2 In-Memory Database (`jdbc:h2:mem:aerofleetdb`)
- **Frontend**: HTML5 Canvas, Tailwind CSS, Lucide Icons, SockJS, StompJS
- **Build System**: Maven Wrapper (`mvnw.cmd` / `mvnw`)

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java**: JDK 17, 21, or 24 installed
- **Terminal**: Windows PowerShell, Command Prompt, or Linux/macOS Bash

### 1. Build and Run the Platform
From the project root (`e:\AeroFleet`):

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### 2. Open the Mission Control Center
Open your browser and navigate to:
```
http://localhost:8080
```

- **Live Mission Control Dashboard**: `http://localhost:8080`
- **H2 In-Memory Database Console**: `http://localhost:8080/h2-console`
  - **JDBC URL**: `jdbc:h2:mem:aerofleetdb`
  - **User**: `sa`
  - **Password**: *(leave empty)*

---

## 🧪 Running Automated Unit Tests

Execute the complete test suite verifying kinematics, exact geometry projection, battery drain/charging, and Spring Boot context:

```powershell
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### Test Coverage Highlights:
- `NoFlyZoneTest`: Validates exact line-circle projection, small-radius detection, and inactive zone bypass.
- `BatteryDrainModelTest`: Validates discharge under payload, weather drag, and depot charging capping at 100%.
- `KinematicsTest`: Validates Euclidean distance, bearing degrees, and vector stepping.
- `AeroFleetApplicationTests`: Verifies full application context bootstrap and database seeding.

---

## 📡 REST API Summary

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/drones` | List all drones with live coordinates and battery |
| `GET` | `/api/v1/drones/{id}` | Get specific drone details |
| `POST` | `/api/v1/drones/{id}/recall` | Trigger emergency Return-To-Home to nearest hub |
| `GET` | `/api/v1/orders` | List delivery orders (optional `?status=PENDING`) |
| `POST` | `/api/v1/orders` | Create a new delivery order |
| `POST` | `/api/v1/orders/simulate-batch` | Generate randomized test delivery missions |
| `GET` | `/api/v1/depots` | List all base stations and charging pad occupancies |
| `GET` | `/api/v1/geofence` | List all active/inactive No-Fly Zones |
| `POST` | `/api/v1/geofence` | Establish a new circular restricted zone |
| `PUT` | `/api/v1/geofence/{id}/toggle` | Toggle zone active/inactive state |
| `DELETE` | `/api/v1/geofence/{id}` | Remove a single restricted zone |
| `DELETE` | `/api/v1/geofence` | Clear all restricted zones |
| `GET` | `/api/v1/simulation/status` | Read simulation clock, speed multiplier, and weather |
| `POST` | `/api/v1/simulation/control` | Control pause/play, speed (0.2x-20x), and atmosphere |
| `POST` | `/api/v1/simulation/incident` | Inject fault (`BATTERY_DRAIN`, `MOTOR_FAULT`) |

*For complete request/response schemas and curl examples, see **[API Documentation](docs/API_DOCUMENTATION.md)**.*

---

## 🎓 Academic Viva & Presentation Highlights

When demonstrating this project to professors and external examiners:
1. **Pure Cartesian Geometry**: Emphasize that all spatial mechanics (distance, heading, collision detection, and detour waypoints) are implemented from mathematical first principles on an $800 \times 600$ grid.
2. **Exact Point-to-Segment Projection**: Explain the dot-product scalar projection ($t = \text{clamp}(\dots)$) used in `NoFlyZone.java` for $O(1)$ collision checks that never miss narrow zones.
3. **Dynamic In-Flight Replanning**: Demonstrate establishing a No-Fly Zone in front of an active drone and showing the planned flight polyline dynamically bend around the obstacle in real time.
4. **Auto-Dock Charging**: Demonstrate clicking "Drain 30%" on an idle depot drone, watching it transition to `CHARGING`, and observing it smoothly recover to 100%.

---

## 📁 Project Structure

```
AeroFleet/
├── docs/
│   ├── API_DOCUMENTATION.md          # Complete REST & WebSocket reference
│   ├── DESIGN_DOCUMENTATION.md       # Architecture, math models & state diagrams
│   └── CODE_LEVEL_DOCUMENTATION.md   # Class taxonomy, OOP patterns & Viva Q&A
├── src/
│   ├── main/
│   │   ├── java/com/aerofleet/       # Spring Boot source code
│   │   └── resources/
│   │       ├── application.yml       # Simulation configuration
│   │       ├── data.sql              # Seed hubs, drones, zones, and orders
│   │       └── static/               # Frontend HTML5, CSS & JS radar
│   └── test/java/com/aerofleet/      # Automated unit test suite
├── pom.xml                           # Maven dependencies & build definition
└── README.md                         # Master entry point
```
