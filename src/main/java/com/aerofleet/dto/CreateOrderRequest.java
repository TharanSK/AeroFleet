package com.aerofleet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateOrderRequest {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotNull(message = "Delivery latitude is required")
    private Double deliveryLatitude;

    @NotNull(message = "Delivery longitude is required")
    private Double deliveryLongitude;

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

    public Double getDeliveryLatitude() {
        return deliveryLatitude;
    }

    public void setDeliveryLatitude(Double deliveryLatitude) {
        this.deliveryLatitude = deliveryLatitude;
    }

    public Double getDeliveryLongitude() {
        return deliveryLongitude;
    }

    public void setDeliveryLongitude(Double deliveryLongitude) {
        this.deliveryLongitude = deliveryLongitude;
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
