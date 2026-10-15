package com.aerofleet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for defining a new restricted No-Fly Zone on the 2D grid.
 */
public class CreateZoneRequest {

    @NotBlank(message = "Zone name is required")
    private String name;

    @NotNull(message = "Center X coordinate is required")
    @DecimalMin(value = "0.0", message = "X coordinate must be >= 0")
    @DecimalMax(value = "800.0", message = "X coordinate must be <= 800")
    private Double centerX;

    @NotNull(message = "Center Y coordinate is required")
    @DecimalMin(value = "0.0", message = "Y coordinate must be >= 0")
    @DecimalMax(value = "600.0", message = "Y coordinate must be <= 600")
    private Double centerY;

    @NotNull(message = "Radius is required")
    @DecimalMin(value = "15.0", message = "Radius must be at least 15 units")
    @DecimalMax(value = "250.0", message = "Radius cannot exceed 250 units")
    private Double radius;

    private boolean active = true;

    public CreateZoneRequest() {
    }

    public CreateZoneRequest(String name, Double centerX, Double centerY, Double radius, boolean active) {
        this.name = name;
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getCenterX() {
        return centerX;
    }

    public void setCenterX(Double centerX) {
        this.centerX = centerX;
    }

    public Double getCenterY() {
        return centerY;
    }

    public void setCenterY(Double centerY) {
        this.centerY = centerY;
    }

    public Double getRadius() {
        return radius;
    }

    public void setRadius(Double radius) {
        this.radius = radius;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
