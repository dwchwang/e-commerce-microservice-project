package com.ecommerce.order.service;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(false)
                    .build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
    }
}
