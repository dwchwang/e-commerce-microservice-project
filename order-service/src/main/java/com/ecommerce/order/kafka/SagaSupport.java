package com.ecommerce.order.kafka;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.event.OrderItemEvent;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.ProcessedEvent;
import com.ecommerce.order.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SagaSupport {

    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    public <T> T parse(String payload, Class<T> type, String topic) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid " + topic + " payload", ex);
        }
    }

    public boolean alreadyProcessed(UUID eventId) {
        return processedEventRepository.existsById(eventId);
    }

    public void markProcessed(UUID eventId) {
        processedEventRepository.save(new ProcessedEvent(eventId));
    }

    public List<OrderItemEvent> toItemEvents(Order order) {
        return order.getItems().stream()
                .map(this::toItemEvent)
                .toList();
    }

    public void requireSuccess(ApiResponse<?> response, String action) {
        if (response == null || !response.isSuccess()) {
            throw new BusinessException(action + " failed: "
                    + (response != null ? response.getMessage() : "No response"));
        }
    }

    private OrderItemEvent toItemEvent(OrderItem item) {
        return OrderItemEvent.builder()
                .productId(item.getProductId().toString())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
