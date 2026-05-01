package com.ecommerce.payment.kafka;

import com.ecommerce.common.event.PaymentRequestedEvent;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestedConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @KafkaListener(topics = "payment-requested", groupId = "payment-service")
    public void handle(String payload) {
        try {
            paymentService.completeCodPayment(objectMapper.readValue(payload, PaymentRequestedEvent.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid payment-requested payload", ex);
        }
    }
}
