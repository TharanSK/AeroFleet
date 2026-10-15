-- Seed Depots
INSERT INTO depots (id, name, latitude, longitude, elevation_meters, charging_pads_total, charging_pads_occupied)
VALUES (1, 'Central Aerodrome Hub', 37.7749, -122.4194, 15.0, 6, 3);

INSERT INTO depots (id, name, latitude, longitude, elevation_meters, charging_pads_total, charging_pads_occupied)
VALUES (2, 'Mission Bay Marine Port', 37.7698, -122.3888, 5.0, 4, 2);

INSERT INTO depots (id, name, latitude, longitude, elevation_meters, charging_pads_total, charging_pads_occupied)
VALUES (3, 'Presidio Staging Base', 37.7985, -122.4660, 30.0, 4, 3);

-- Seed Drones
INSERT INTO drones (id, callsign, model_type, current_latitude, current_longitude, current_altitude_meters, current_speed_mps, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled_meters, successful_deliveries_count)
VALUES (1, 'AF-ALPHA-101', 'QUADCOPTER_STANDARD', 37.7749, -122.4194, 0.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 1, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, current_latitude, current_longitude, current_altitude_meters, current_speed_mps, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled_meters, successful_deliveries_count)
VALUES (2, 'AF-ALPHA-102', 'QUADCOPTER_STANDARD', 37.7749, -122.4194, 0.0, 0.0, 0.0, 95.0, 0.0, 'IDLE', 1, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, current_latitude, current_longitude, current_altitude_meters, current_speed_mps, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled_meters, successful_deliveries_count)
VALUES (3, 'AF-HEAVY-201', 'HEXACOPTER_HEAVY', 37.7749, -122.4194, 0.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 1, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, current_latitude, current_longitude, current_altitude_meters, current_speed_mps, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled_meters, successful_deliveries_count)
VALUES (4, 'AF-HEAVY-202', 'HEXACOPTER_HEAVY', 37.7698, -122.3888, 0.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 2, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, current_latitude, current_longitude, current_altitude_meters, current_speed_mps, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled_meters, successful_deliveries_count)
VALUES (5, 'AF-WING-301', 'VTOL_FIXED_WING', 37.7698, -122.3888, 0.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 2, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, current_latitude, current_longitude, current_altitude_meters, current_speed_mps, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled_meters, successful_deliveries_count)
VALUES (6, 'AF-WING-302', 'VTOL_FIXED_WING', 37.7985, -122.4660, 0.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 3, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, current_latitude, current_longitude, current_altitude_meters, current_speed_mps, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled_meters, successful_deliveries_count)
VALUES (7, 'AF-ALPHA-103', 'QUADCOPTER_STANDARD', 37.7985, -122.4660, 0.0, 0.0, 0.0, 88.0, 0.0, 'IDLE', 3, NULL, 0, 0.0, 0);

INSERT INTO drones (id, callsign, model_type, current_latitude, current_longitude, current_altitude_meters, current_speed_mps, heading_degrees, battery_percent, current_payload_kg, status, current_depot_id, assigned_order_id, total_flight_time_seconds, total_distance_traveled_meters, successful_deliveries_count)
VALUES (8, 'AF-HEAVY-203', 'HEXACOPTER_HEAVY', 37.7985, -122.4660, 0.0, 0.0, 0.0, 100.0, 0.0, 'IDLE', 3, NULL, 0, 0.0, 0);

-- Seed No-Fly Zones
INSERT INTO no_fly_zones (id, name, center_latitude, center_longitude, radius_meters, min_altitude_meters, max_altitude_meters, active)
VALUES (1, 'Downtown Heliport Exclusion Zone', 37.7850, -122.4050, 650.0, 0.0, 250.0, true);

INSERT INTO no_fly_zones (id, name, center_latitude, center_longitude, radius_meters, min_altitude_meters, max_altitude_meters, active)
VALUES (2, 'City Memorial Hospital Air Corridor', 37.7550, -122.4350, 550.0, 0.0, 200.0, true);

-- Seed Initial Delivery Orders
INSERT INTO delivery_orders (id, order_number, customer_name, delivery_latitude, delivery_longitude, package_weight_kg, priority, status, assigned_drone_id, origin_depot_id, created_at, delivered_at)
VALUES (1, 'ORD-9001', 'Pacific Heights Medical Labs', 37.7915, -122.4350, 1.4, 'MEDICAL', 'PENDING', NULL, 1, CURRENT_TIMESTAMP, NULL);

INSERT INTO delivery_orders (id, order_number, customer_name, delivery_latitude, delivery_longitude, package_weight_kg, priority, status, assigned_drone_id, origin_depot_id, created_at, delivered_at)
VALUES (2, 'ORD-9002', 'Potrero Hill Tech Campus', 37.7580, -122.4020, 3.2, 'STANDARD', 'PENDING', NULL, 2, CURRENT_TIMESTAMP, NULL);
