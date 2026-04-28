package com.ecommerce.inventory.kafka;

import com.ecommerce.common.event.OrderCancelledEvent;
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

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledConsumer {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "order-cancelled", groupId = "inventory-service")
    public void handleOrderCancelled(String payload) {
        OrderCancelledEvent event = readEvent(payload);
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

        for (Map.Entry<String, Integer> entry : requestedBySku.entrySet()) {
            Inventory inventory = inventoryRepository.findBySkuForUpdate(entry.getKey())
                    .orElseThrow(() -> new IllegalStateException("Missing inventory for SKU " + entry.getKey()));
            if (inventory.getReservedQuantity() < entry.getValue()) {
                throw new IllegalStateException("Reserved quantity invariant violated for SKU " + entry.getKey());
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() - entry.getValue());
            inventoryRepository.save(inventory);

            stockMovementRepository.save(StockMovement.builder()
                    .sku(entry.getKey())
                    .movementType(MovementType.RELEASE)
                    .quantity(entry.getValue())
                    .referenceId(event.getOrderId().toString())
                    .build());
        }

        processedEventRepository.save(new ProcessedEvent(event.getEventId()));
        log.info("Released reserved inventory for cancelled order {}", event.getOrderId());
    }

    private OrderCancelledEvent readEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderCancelledEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid order-cancelled event payload", ex);
        }
    }
}
