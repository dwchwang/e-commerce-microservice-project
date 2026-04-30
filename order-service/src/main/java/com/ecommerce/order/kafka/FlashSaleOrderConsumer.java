package com.ecommerce.order.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FlashSaleOrderConsumer {

    @KafkaListener(topics = "flash-sale-order-requested", groupId = "order-service")
    public void handle(String payload) {
        log.warn("[PHASE-7-STUB] Received flash-sale-order-requested event. Payload ignored: {}", payload);
    }
}
