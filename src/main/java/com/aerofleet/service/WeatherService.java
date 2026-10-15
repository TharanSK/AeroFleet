package com.aerofleet.service;

import com.aerofleet.domain.enums.WeatherCondition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private WeatherCondition condition = WeatherCondition.CLEAR;
    private double windSpeedMps = 4.0;
    private double windDirectionDeg = 45.0; // Coming from NE

    public WeatherService(
            @Value("${aerofleet.simulation.weather.condition:CLEAR}") String initialCondition,
            @Value("${aerofleet.simulation.weather.wind-speed-mps:4.0}") double initialWindSpeed,
            @Value("${aerofleet.simulation.weather.wind-direction-deg:45.0}") double initialWindDirection) {
        try {
            this.condition = WeatherCondition.valueOf(initialCondition.toUpperCase());
        } catch (Exception e) {
            this.condition = WeatherCondition.CLEAR;
        }
        this.windSpeedMps = initialWindSpeed;
        this.windDirectionDeg = initialWindDirection;
    }

    public synchronized WeatherCondition getCondition() {
        return condition;
    }

    public synchronized void setCondition(WeatherCondition condition) {
        this.condition = condition;
    }

    public synchronized double getWindSpeedMps() {
        return windSpeedMps;
    }

    public synchronized void setWindSpeedMps(double windSpeedMps) {
        this.windSpeedMps = Math.max(0.0, windSpeedMps);
    }

    public synchronized double getWindDirectionDeg() {
        return windDirectionDeg;
    }

    public synchronized void setWindDirectionDeg(double windDirectionDeg) {
        this.windDirectionDeg = (windDirectionDeg + 360.0) % 360.0;
    }

    public synchronized double getDragMultiplier() {
        return condition.getDragMultiplier() + (windSpeedMps / 20.0);
    }
}
