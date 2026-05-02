package com.ecommerce.search.kafka;

import com.ecommerce.common.event.ProductCreatedEvent;
import com.ecommerce.common.event.ProductDeletedEvent;
import com.ecommerce.common.event.ProductUpdatedEvent;
import com.ecommerce.search.service.ProductIndexService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProductIndexService productIndexService;

    @KafkaListener(topics = "product-created", groupId = "search-service")
    public void handleProductCreated(String payload) {
        ProductCreatedEvent event = read(payload, ProductCreatedEvent.class, "product-created");
        productIndexService.indexCreated(event);
    }

    @KafkaListener(topics = "product-updated", groupId = "search-service")
    public void handleProductUpdated(String payload) {
        ProductUpdatedEvent event = read(payload, ProductUpdatedEvent.class, "product-updated");
        productIndexService.indexUpdated(event);
    }

    @KafkaListener(topics = "product-deleted", groupId = "search-service")
    public void handleProductDeleted(String payload) {
        ProductDeletedEvent event = read(payload, ProductDeletedEvent.class, "product-deleted");
        productIndexService.deleteOrDeactivate(event);
    }

    private <T> T read(String payload, Class<T> type, String topic) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException ex) {
            log.warn("Invalid {} payload: {}", topic, payload);
            throw new IllegalArgumentException("Invalid " + topic + " payload", ex);
        }
    }
}
