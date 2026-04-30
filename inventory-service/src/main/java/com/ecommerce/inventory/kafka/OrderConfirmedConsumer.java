package com.ecommerce.inventory.kafka;

import com.ecommerce.common.event.OrderConfirmedEvent;
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
public class OrderConfirmedConsumer {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "order-confirmed", groupId = "inventory-service")
    public void handleOrderConfirmed(String payload) {
        OrderConfirmedEvent event = readEvent(payload);
        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return;
        }

        Map<String, Integer> confirmedBySku = event.getItems().stream()
                .collect(Collectors.toMap(
                        OrderItemEvent::getSku,
                        OrderItemEvent::getQuantity,
                        Integer::sum,
                        TreeMap::new));

        for (Map.Entry<String, Integer> entry : confirmedBySku.entrySet()) {
            Inventory inventory = inventoryRepository.findBySkuForUpdate(entry.getKey())
                    .orElseThrow(() -> new IllegalStateException("Missing inventory for SKU " + entry.getKey()));

            if (inventory.getReservedQuantity() < entry.getValue()) {
                throw new IllegalStateException("Reserved quantity invariant violated for SKU " + entry.getKey());
            }
            if (inventory.getQuantity() < entry.getValue()) {
                throw new IllegalStateException("Quantity invariant violated for SKU " + entry.getKey());
            }

            inventory.setQuantity(inventory.getQuantity() - entry.getValue());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - entry.getValue());
            inventoryRepository.save(inventory);

            stockMovementRepository.save(StockMovement.builder()
                    .sku(entry.getKey())
                    .movementType(MovementType.STOCK_OUT)
                    .quantity(entry.getValue())
                    .referenceId(event.getOrderId().toString())
                    .note("Order confirmed - payment success")
                    .build());
        }

        processedEventRepository.save(new ProcessedEvent(event.getEventId()));
        log.info("Stock finalized for confirmed order {}", event.getOrderId());
    }

    private OrderConfirmedEvent readEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderConfirmedEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid order-confirmed event payload", ex);
        }
    }
}
