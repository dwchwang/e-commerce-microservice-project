package com.ecommerce.flashsale.kafka;

import com.ecommerce.common.event.FlashSaleOrderRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FlashSaleEventProducer {

    private static final String TOPIC = "flash-sale-order-requested";
    private static final long PUBLISH_TIMEOUT_SECONDS = 3;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishSync(FlashSaleOrderRequestedEvent event) throws Exception {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(TOPIC, event.getFlashSaleId().toString(), payload)
                .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
