package com.ecommerce.order.kafka;

import com.ecommerce.common.event.InventoryFailedEvent;
import com.ecommerce.common.event.OrderCancelledEvent;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.client.VoucherServiceClient;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryFailedConsumer {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final VoucherServiceClient voucherClient;
    private final SagaSupport sagaSupport;

    @Transactional
    @KafkaListener(topics = "inventory-failed", groupId = "order-service")
    public void handle(String payload) {
        InventoryFailedEvent event = sagaSupport.parse(payload, InventoryFailedEvent.class, "inventory-failed");
        if (sagaSupport.alreadyProcessed(event.getEventId())) {
            return;
        }

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", event.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Stale inventory-failed event for order {} with status {}; ignoring",
                    event.getOrderId(), order.getStatus());
            sagaSupport.markProcessed(event.getEventId());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason("Insufficient stock: " + event.getReason());

        if (order.getVoucherCode() != null) {
            sagaSupport.requireSuccess(voucherClient.release(order.getVoucherCode(), order.getId()),
                    "Release voucher " + order.getVoucherCode());
        }

        outboxService.saveEvent("Order", order.getId().toString(), "ORDER_CANCELLED",
                OrderCancelledEvent.builder()
                        .eventId(UUID.randomUUID())
                        .orderId(order.getId())
                        .userId(order.getUserId())
                        .userEmail(order.getUserEmail())
                        .reason("Insufficient stock: " + event.getReason())
                        .items(List.of())
                        .build());

        orderRepository.save(order);
        sagaSupport.markProcessed(event.getEventId());
        log.info("Order {} cancelled due to inventory failure", order.getId());
    }
}
