package com.ecommerce.order.scheduler;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.repository.OutboxRepository;
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
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1_000)
    @Transactional
    public void pollOutbox() {
        List<OutboxEvent> events = outboxRepository.findUnprocessedBatch();
        for (OutboxEvent event : events) {
            String topic = resolveTopic(event.getEventType());
            try {
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.info("Published outbox event {} ({}) to topic {}", event.getId(), event.getEventType(), topic);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {} to topic {}; stopping batch",
                        event.getId(), topic, ex);
                break;
            }
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "ORDER_CREATED" -> "order-created";
            case "PAYMENT_REQUESTED" -> "payment-requested";
            case "ORDER_CANCELLED" -> "order-cancelled";
            case "ORDER_CONFIRMED" -> "order-confirmed";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
