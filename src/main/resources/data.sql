-- Seed Charging Depots on 800x600 Grid
INSERT INTO depots (id, name, x, y, charging_pads_total, charging_pads_occupied)
VALUES (1, 'Central Hub Alpha', 400.0, 300.0, 6, 3);

INSERT INTO depots (id, name, x, y, charging_pads_total, charging_pads_occupied)
VALUES (2, 'West Station Beta', 150.0, 180.0, 4, 2);

INSERT INTO depots (id, name, x, y, charging_pads_total, charging_pads_occupied)
VALUES (3, 'East Station Gamma', 650.0, 420.0, 4, 3);

-- Seed Drones on Grid
INSERT INTO drones (id, callsign, model_type, x, y, speed, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled, successful_deliveries_count)
VALUES (1, 'AF-ALPHA-101', 'QUADCOPTER_STANDARD', 400.0, 300.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 1, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, x, y, speed, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled, successful_deliveries_count)
VALUES (2, 'AF-ALPHA-102', 'QUADCOPTER_STANDARD', 400.0, 300.0, 0.0, 0.0, 95.0, 0.0, 'IDLE', 1, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, x, y, speed, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled, successful_deliveries_count)
VALUES (3, 'AF-HEAVY-201', 'HEXACOPTER_HEAVY', 400.0, 300.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 1, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, x, y, speed, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled, successful_deliveries_count)
VALUES (4, 'AF-HEAVY-202', 'HEXACOPTER_HEAVY', 150.0, 180.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 2, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, x, y, speed, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled, successful_deliveries_count)
VALUES (5, 'AF-WING-301', 'VTOL_FIXED_WING', 150.0, 180.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 2, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, x, y, speed, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled, successful_deliveries_count)
VALUES (6, 'AF-WING-302', 'VTOL_FIXED_WING', 650.0, 420.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 3, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, x, y, speed, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled, successful_deliveries_count)
VALUES (7, 'AF-ALPHA-103', 'QUADCOPTER_STANDARD', 650.0, 420.0, 0.0, 0.0, 90.0, 0.0, 'IDLE', 3, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, x, y, speed, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled, successful_deliveries_count)
VALUES (8, 'AF-HEAVY-203', 'HEXACOPTER_HEAVY', 650.0, 420.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 3, NULL, 0, 0.0, 0);

-- Seed No-Fly Zones on Grid
INSERT INTO no_fly_zones (id, name, center_x, center_y, radius, active)
VALUES (1, 'Hospital Helipad Zone', 280.0, 320.0, 55.0, true);

INSERT INTO no_fly_zones (id, name, center_x, center_y, radius, active)
VALUES (2, 'High-Rise Tower Exclusion', 520.0, 200.0, 65.0, true);

-- Seed Initial Delivery Orders
INSERT INTO delivery_orders (id, order_number, customer_name, delivery_x, delivery_y, package_weight_kg, priority, status, assigned_drone_id, origin_depot_id, created_at, delivered_at)
VALUES (1, 'ORD-1001', 'Science Library', 200.0, 450.0, 1.5, 'MEDICAL', 'PENDING', NULL, 1, CURRENT_TIMESTAMP, NULL);

INSERT INTO delivery_orders (id, order_number, customer_name, delivery_x, delivery_y, package_weight_kg, priority, status, assigned_drone_id, origin_depot_id, created_at, delivered_at)
VALUES (2, 'ORD-1002', 'North Dormitory', 600.0, 150.0, 2.0, 'STANDARD', 'PENDING', NULL, 3, CURRENT_TIMESTAMP, NULL);

-- Advance auto-increment sequences past initial seed data
ALTER TABLE delivery_orders ALTER COLUMN id RESTART WITH 100;
ALTER TABLE drones ALTER COLUMN id RESTART WITH 100;
ALTER TABLE depots ALTER COLUMN id RESTART WITH 100;
ALTER TABLE no_fly_zones ALTER COLUMN id RESTART WITH 100;
