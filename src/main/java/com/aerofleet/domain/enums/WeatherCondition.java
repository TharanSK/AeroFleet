package com.aerofleet.domain.enums;

public enum WeatherCondition {
    CLEAR(1.0, "Clear skies, optimal flight conditions"),
    WINDY(1.3, "High headwinds, increased battery drain"),
    RAIN(1.5, "Moderate rain, reduced sensor range and higher drag"),
    STORM(2.5, "Severe storm, flights grounded / emergency recall");

    private final double dragMultiplier;
    private final String description;

    WeatherCondition(double dragMultiplier, String description) {
        this.dragMultiplier = dragMultiplier;
        this.description = description;
    }

    public double getDragMultiplier() {
        return dragMultiplier;
    }

    public String getDescription() {
        return description;
    }
}
