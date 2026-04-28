package com.ecommerce.inventory.kafka;

import com.ecommerce.common.event.InventoryFailedEvent;
import com.ecommerce.common.event.InventoryUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUpdated(UUID orderId) {
        kafkaTemplate.send("inventory-updated", orderId.toString(), InventoryUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .build());
    }

    public void publishFailed(UUID orderId, String reason) {
        kafkaTemplate.send("inventory-failed", orderId.toString(), InventoryFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(orderId)
                .reason(reason)
                .build());
    }
}
