package com.aerofleet.controller;

import com.aerofleet.domain.model.DeliveryOrder;
import com.aerofleet.dto.AlertEventDTO;
import com.aerofleet.dto.FleetSnapshotDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class TelemetryBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public TelemetryBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastFleetSnapshot(FleetSnapshotDTO snapshot) {
        messagingTemplate.convertAndSend("/topic/telemetry", snapshot);
    }

    public void broadcastAlert(AlertEventDTO alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }

    public void broadcastOrderUpdate(DeliveryOrder order) {
        messagingTemplate.convertAndSend("/topic/orders", order);
    }
}
