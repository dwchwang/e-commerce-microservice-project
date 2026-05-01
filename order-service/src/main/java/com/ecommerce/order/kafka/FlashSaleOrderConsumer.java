package com.ecommerce.order.kafka;

import com.ecommerce.common.event.FlashSaleOrderRequestedEvent;
import com.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashSaleOrderConsumer {

    private final SagaSupport sagaSupport;
    private final OrderService orderService;

    @KafkaListener(topics = "flash-sale-order-requested", groupId = "order-service")
    @Transactional
    public void handle(String payload) {
        FlashSaleOrderRequestedEvent event = sagaSupport.parse(
                payload, FlashSaleOrderRequestedEvent.class, "flash-sale-order-requested");
        if (event.getEventId() == null) {
            throw new IllegalArgumentException("flash-sale-order-requested eventId is required");
        }
        if (sagaSupport.alreadyProcessed(event.getEventId())) {
            log.info("Skipping duplicate flash sale order event {}", event.getEventId());
            return;
        }

        orderService.createFlashSaleOrder(event);
        sagaSupport.markProcessed(event.getEventId());
        log.info("Created flash sale order for campaign {} and user {}",
                event.getFlashSaleId(), event.getUserId());
    }
}
