package com.aerofleet.dto;

import java.time.LocalDateTime;

public class AlertEventDTO {
    private String id;
    private String timestamp;
    private String level; // "INFO", "SUCCESS", "WARN", "DANGER"
    private String title;
    private String message;
    private Long droneId;
    private Long orderId;

    public AlertEventDTO() {
        this.timestamp = LocalDateTime.now().toString();
    }

    public AlertEventDTO(String level, String title, String message, Long droneId, Long orderId) {
        this();
        this.id = System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        this.level = level;
        this.title = title;
        this.message = message;
        this.droneId = droneId;
        this.orderId = orderId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getDroneId() {
        return droneId;
    }

    public void setDroneId(Long droneId) {
        this.droneId = droneId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
