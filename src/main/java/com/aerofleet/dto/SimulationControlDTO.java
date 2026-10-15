package com.aerofleet.dto;

public class SimulationControlDTO {
    private Boolean running;
    private Double speedMultiplier;
    private String weatherCondition; // CLEAR, WINDY, RAIN, STORM

    public SimulationControlDTO() {
    }

    public Boolean getRunning() {
        return running;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }

    public Double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setSpeedMultiplier(Double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }
}
