package com.ecommerce.notification.kafka;

import com.ecommerce.common.event.OrderCancelledEvent;
import com.ecommerce.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCancelledConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "order-cancelled", groupId = "notification-service")
    public void handle(String payload) {
        try {
            notificationService.sendOrderCancelled(objectMapper.readValue(payload, OrderCancelledEvent.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid order-cancelled payload", ex);
        }
    }
}
