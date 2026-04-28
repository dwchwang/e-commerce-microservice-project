package com.ecommerce.inventory.kafka;

import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.OrderItemEvent;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.MovementType;
import com.ecommerce.inventory.entity.ProcessedEvent;
import com.ecommerce.inventory.entity.StockMovement;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.ProcessedEventRepository;
import com.ecommerce.inventory.repository.StockMovementRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryEventProducer inventoryEventProducer;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "order-created", groupId = "inventory-service")
    public void handleOrderCreated(String payload) {
        OrderCreatedEvent event = readEvent(payload);
        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return;
        }

        Map<String, Integer> requestedBySku = event.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItemEvent::getSku,
                        OrderItemEvent::getQuantity,
                        Integer::sum,
                        TreeMap::new));

        Map<String, Inventory> lockedBySku = new LinkedHashMap<>();
        for (String sku : requestedBySku.keySet()) {
            lockedBySku.put(sku, inventoryRepository.findBySkuForUpdate(sku).orElse(null));
        }

        for (Map.Entry<String, Integer> entry : requestedBySku.entrySet()) {
            Inventory inventory = lockedBySku.get(entry.getKey());
            int available = inventory != null ? inventory.availableQuantity() : 0;
            if (available < entry.getValue()) {
                String reason = "Insufficient stock for SKU: " + entry.getKey()
                        + " (available: " + available + ", requested: " + entry.getValue() + ")";
                inventoryEventProducer.publishFailed(event.getOrderId(), reason);
                processedEventRepository.save(new ProcessedEvent(event.getEventId()));
                log.info("Inventory reservation failed for order {}: {}", event.getOrderId(), reason);
                return;
            }
        }

        for (Map.Entry<String, Integer> entry : requestedBySku.entrySet()) {
            Inventory inventory = lockedBySku.get(entry.getKey());
            inventory.setReservedQuantity(inventory.getReservedQuantity() + entry.getValue());
            inventoryRepository.save(inventory);

            stockMovementRepository.save(StockMovement.builder()
                    .sku(entry.getKey())
                    .movementType(MovementType.RESERVE)
                    .quantity(entry.getValue())
                    .referenceId(event.getOrderId().toString())
                    .build());
        }

        inventoryEventProducer.publishUpdated(event.getOrderId());
        processedEventRepository.save(new ProcessedEvent(event.getEventId()));
        log.info("Reserved inventory for order {}", event.getOrderId());
    }

    private OrderCreatedEvent readEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderCreatedEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid order-created event payload", ex);
        }
    }
}
