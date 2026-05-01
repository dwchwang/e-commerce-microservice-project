package com.ecommerce.order.kafka;

import com.ecommerce.common.event.InventoryUpdatedEvent;
import com.ecommerce.common.event.PaymentRequestedEvent;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentMethod;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryUpdatedConsumer {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final SagaSupport sagaSupport;

    @Transactional
    @KafkaListener(topics = "inventory-updated", groupId = "order-service")
    public void handle(String payload) {
        InventoryUpdatedEvent event = sagaSupport.parse(payload, InventoryUpdatedEvent.class, "inventory-updated");
        if (sagaSupport.alreadyProcessed(event.getEventId())) {
            return;
        }

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", event.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Stale inventory-updated event for order {} with status {}; ignoring",
                    event.getOrderId(), order.getStatus());
            sagaSupport.markProcessed(event.getEventId());
            return;
        }

        order.setStatus(OrderStatus.STOCK_RESERVED);
        if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
            order.setReservationExpiredAt(LocalDateTime.now().plusMinutes(30));
        }
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            outboxService.saveEvent("Order", order.getId().toString(), "PAYMENT_REQUESTED",
                    PaymentRequestedEvent.builder()
                            .eventId(UUID.randomUUID())
                            .orderId(order.getId())
                            .userId(order.getUserId())
                            .userEmail(order.getUserEmail())
                            .amount(order.getTotalAmount())
                            .paymentMethod("COD")
                            .build());
        }

        orderRepository.save(order);
        sagaSupport.markProcessed(event.getEventId());
        log.info("Order {} moved to STOCK_RESERVED", order.getId());
    }
}
