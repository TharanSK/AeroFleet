package com.aerofleet.domain.model;

import com.aerofleet.domain.enums.OrderStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_orders")
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNumber;
    private String customerName;
    private double deliveryLatitude;
    private double deliveryLongitude;
    private double packageWeightKg;
    private String priority; // STANDARD, EXPRESS, MEDICAL

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Long assignedDroneId;
    private Long originDepotId;

    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;

    public DeliveryOrder() {
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.priority = "STANDARD";
    }

    public DeliveryOrder(String orderNumber, String customerName, double deliveryLatitude, double deliveryLongitude, double packageWeightKg, String priority, Long originDepotId) {
        this();
        this.orderNumber = orderNumber;
        this.customerName = customerName;
        this.deliveryLatitude = deliveryLatitude;
        this.deliveryLongitude = deliveryLongitude;
        this.packageWeightKg = packageWeightKg;
        this.priority = priority != null ? priority : "STANDARD";
        this.originDepotId = originDepotId;
    }

    public GeoCoordinate getDeliveryCoordinate() {
        return new GeoCoordinate(deliveryLatitude, deliveryLongitude, 0.0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getDeliveryLatitude() {
        return deliveryLatitude;
    }

    public void setDeliveryLatitude(double deliveryLatitude) {
        this.deliveryLatitude = deliveryLatitude;
    }

    public double getDeliveryLongitude() {
        return deliveryLongitude;
    }

    public void setDeliveryLongitude(double deliveryLongitude) {
        this.deliveryLongitude = deliveryLongitude;
    }

    public double getPackageWeightKg() {
        return packageWeightKg;
    }

    public void setPackageWeightKg(double packageWeightKg) {
        this.packageWeightKg = packageWeightKg;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Long getAssignedDroneId() {
        return assignedDroneId;
    }

    public void setAssignedDroneId(Long assignedDroneId) {
        this.assignedDroneId = assignedDroneId;
    }

    public Long getOriginDepotId() {
        return originDepotId;
    }

    public void setOriginDepotId(Long originDepotId) {
        this.originDepotId = originDepotId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
