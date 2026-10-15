package com.aerofleet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Customer order request submitted from the UI.
 */
public class CreateOrderRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotNull(message = "Delivery X coordinate is required")
    @DecimalMin(value = "0.0", message = "X coordinate must be >= 0")
    @DecimalMax(value = "800.0", message = "X coordinate must be <= 800")
    private Double deliveryX;

    @NotNull(message = "Delivery Y coordinate is required")
    @DecimalMin(value = "0.0", message = "Y coordinate must be >= 0")
    @DecimalMax(value = "600.0", message = "Y coordinate must be <= 600")
    private Double deliveryY;

    @NotNull(message = "Package weight is required")
    @DecimalMin(value = "0.1", message = "Minimum package weight is 0.1 kg")
    @DecimalMax(value = "10.0", message = "Maximum drone capacity is 10.0 kg")
    private Double packageWeightKg;

    private String priority; // STANDARD, EXPRESS, MEDICAL
    private Long originDepotId;

    public CreateOrderRequest() {
        this.priority = "STANDARD";
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Double getDeliveryX() {
        return deliveryX;
    }

    public void setDeliveryX(Double deliveryX) {
        this.deliveryX = deliveryX;
    }

    public Double getDeliveryY() {
        return deliveryY;
    }

    public void setDeliveryY(Double deliveryY) {
        this.deliveryY = deliveryY;
    }

    public Double getPackageWeightKg() {
        return packageWeightKg;
    }

    public void setPackageWeightKg(Double packageWeightKg) {
        this.packageWeightKg = packageWeightKg;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getOriginDepotId() {
        return originDepotId;
    }

    public void setOriginDepotId(Long originDepotId) {
        this.originDepotId = originDepotId;
    }
}
