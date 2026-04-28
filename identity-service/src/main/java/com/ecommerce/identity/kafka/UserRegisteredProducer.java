package com.ecommerce.identity.kafka;

import com.ecommerce.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegisteredProducer {

    private static final String TOPIC = "user-registered";

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public void send(UserRegisteredEvent event) {
        kafkaTemplate.send(TOPIC, event.getUserId(), event);
    }
}
