package com.aerofleet.dto;

public class SimulationControlDTO {
    private Boolean running;
    private Double speedMultiplier;
    private String weatherCondition; // CLEAR, WINDY, RAIN, STORM
    private Double windSpeedMps;
    private Double windDirectionDeg;

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

    public Double getWindSpeedMps() {
        return windSpeedMps;
    }

    public void setWindSpeedMps(Double windSpeedMps) {
        this.windSpeedMps = windSpeedMps;
    }

    public Double getWindDirectionDeg() {
        return windDirectionDeg;
    }

    public void setWindDirectionDeg(Double windDirectionDeg) {
        this.windDirectionDeg = windDirectionDeg;
    }
}
