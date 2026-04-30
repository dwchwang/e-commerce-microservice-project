package com.ecommerce.order.kafka;

import com.ecommerce.common.event.OrderConfirmedEvent;
import com.ecommerce.common.event.PaymentSuccessEvent;
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

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessConsumer {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final VoucherServiceClient voucherClient;
    private final SagaSupport sagaSupport;

    @Transactional
    @KafkaListener(topics = "payment-success", groupId = "order-service")
    public void handle(String payload) {
        PaymentSuccessEvent event = sagaSupport.parse(payload, PaymentSuccessEvent.class, "payment-success");
        if (sagaSupport.alreadyProcessed(event.getEventId())) {
            return;
        }

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", event.getOrderId()));

        if (order.getStatus() != OrderStatus.STOCK_RESERVED) {
            log.warn("Stale payment-success event for order {} with status {}; ignoring",
                    event.getOrderId(), order.getStatus());
            sagaSupport.markProcessed(event.getEventId());
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        if (order.getVoucherCode() != null) {
            sagaSupport.requireSuccess(voucherClient.commit(order.getVoucherCode(), order.getId()),
                    "Commit voucher " + order.getVoucherCode());
        }

        outboxService.saveEvent("Order", order.getId().toString(), "ORDER_CONFIRMED",
                OrderConfirmedEvent.builder()
                        .eventId(UUID.randomUUID())
                        .orderId(order.getId())
                        .userId(order.getUserId())
                        .items(sagaSupport.toItemEvents(order))
                        .build());

        orderRepository.save(order);
        sagaSupport.markProcessed(event.getEventId());
        log.info("Order {} confirmed", order.getId());
    }
}
