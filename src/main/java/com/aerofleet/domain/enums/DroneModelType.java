package com.aerofleet.domain.enums;

public enum DroneModelType {
    QUADCOPTER_STANDARD("AeroQuad X4", 3.0, 35.0, 450.0, 60.0),
    HEXACOPTER_HEAVY("AeroHex HeavyLift", 8.0, 28.0, 850.0, 75.0),
    VTOL_FIXED_WING("AeroWing LongRange", 4.5, 50.0, 700.0, 90.0);

    private final String displayName;
    private final double maxPayloadKg;
    private final double maxSpeedMps;
    private final double batteryCapacityWh;
    private final double cruiseAltitudeMeters;

    DroneModelType(String displayName, double maxPayloadKg, double maxSpeedMps, double batteryCapacityWh, double cruiseAltitudeMeters) {
        this.displayName = displayName;
        this.maxPayloadKg = maxPayloadKg;
        this.maxSpeedMps = maxSpeedMps;
        this.batteryCapacityWh = batteryCapacityWh;
        this.cruiseAltitudeMeters = cruiseAltitudeMeters;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMaxPayloadKg() {
        return maxPayloadKg;
    }

    public double getMaxSpeedMps() {
        return maxSpeedMps;
    }

    public double getBatteryCapacityWh() {
        return batteryCapacityWh;
    }

    public double getCruiseAltitudeMeters() {
        return cruiseAltitudeMeters;
    }
}
