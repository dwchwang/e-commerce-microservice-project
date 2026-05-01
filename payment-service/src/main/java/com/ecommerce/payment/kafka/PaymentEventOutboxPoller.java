package com.ecommerce.payment.kafka;

import com.ecommerce.payment.entity.PaymentOutboxEvent;
import com.ecommerce.payment.entity.PaymentOutboxStatus;
import com.ecommerce.payment.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventOutboxPoller {

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1_000)
    @Transactional
    public void poll() {
        List<PaymentOutboxEvent> events = outboxRepository.findPendingBatch();
        for (PaymentOutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get();
                event.setStatus(PaymentOutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.info("Published payment outbox event {} to topic {}", event.getId(), event.getTopic());
            } catch (Exception ex) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(truncate(errorMessage(ex), MAX_ERROR_LENGTH));
                if (event.getAttempts() >= MAX_ATTEMPTS) {
                    event.setStatus(PaymentOutboxStatus.FAILED);
                }
                outboxRepository.save(event);
                log.error("Failed to publish payment outbox event {} to topic {}", event.getId(), event.getTopic(), ex);
            }
        }
    }

    private String errorMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
