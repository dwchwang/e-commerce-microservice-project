package com.ecommerce.user.kafka;

import com.ecommerce.common.event.UserRegisteredEvent;
import com.ecommerce.user.entity.ProcessedEvent;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.ProcessedEventRepository;
import com.ecommerce.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final UserProfileRepository userProfileRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    @KafkaListener(topics = "user-registered", groupId = "user-service")
    public void handleUserRegistered(UserRegisteredEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return;
        }

        if (!userProfileRepository.existsByKeycloakUserId(event.getUserId())) {
            UserProfile profile = UserProfile.builder()
                    .keycloakUserId(event.getUserId())
                    .email(event.getEmail())
                    .fullName(event.getFullName())
                    .loyaltyPoints(0)
                    .build();
            userProfileRepository.save(profile);
            log.info("Created user profile for keycloakUserId={}", event.getUserId());
        }

        processedEventRepository.save(new ProcessedEvent(event.getEventId()));
    }
}
