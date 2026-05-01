package com.ecommerce.notification.service.impl;

import com.ecommerce.common.event.OrderCancelledEvent;
import com.ecommerce.common.event.OrderConfirmedEvent;
import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.entity.NotificationStatus;
import com.ecommerce.notification.entity.NotificationType;
import com.ecommerce.notification.entity.ProcessedEvent;
import com.ecommerce.notification.repository.NotificationRepository;
import com.ecommerce.notification.repository.ProcessedEventRepository;
import com.ecommerce.notification.service.EmailService;
import com.ecommerce.notification.service.NotificationService;
import com.ecommerce.notification.template.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final EmailService emailService;
    private final EmailTemplateBuilder templates;

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public void sendOrderConfirmed(OrderConfirmedEvent event) {
        send(event.getEventId(), event.getUserId(), event.getUserEmail(), NotificationType.ORDER_CONFIRMED,
                templates.confirmedSubject(event), templates.confirmedBody(event), event.getOrderId());
    }

    @Override
    @Transactional(noRollbackFor = RuntimeException.class)
    public void sendOrderCancelled(OrderCancelledEvent event) {
        send(event.getEventId(), event.getUserId(), event.getUserEmail(), NotificationType.ORDER_CANCELLED,
                templates.cancelledSubject(event), templates.cancelledBody(event), event.getOrderId());
    }

    @Override
    public List<Notification> findByReferenceId(String referenceId) {
        return notificationRepository.findByReferenceIdOrderByCreatedAtDesc(referenceId);
    }

    private void send(UUID eventId, String userId, String email, NotificationType type,
                      String subject, String body, UUID orderId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Notification eventId is required");
        }
        if (processedEventRepository.existsById(eventId)) {
            return;
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Notification email is required");
        }

        Notification notification = notificationRepository.findByEventId(eventId)
                .orElseGet(() -> notificationRepository.save(Notification.builder()
                        .eventId(eventId)
                        .userId(userId)
                        .email(email)
                        .type(type)
                        .subject(subject)
                        .body(body)
                        .status(NotificationStatus.PROCESSING)
                        .referenceId(orderId.toString())
                        .build()));

        if (notification.getStatus() == NotificationStatus.SENT) {
            processedEventRepository.save(new ProcessedEvent(eventId));
            return;
        }

        notification.setStatus(NotificationStatus.PROCESSING);
        notification.setErrorMessage(null);
        notification.setSubject(subject);
        notification.setBody(body);
        notificationRepository.save(notification);

        try {
            emailService.send(email, subject, body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setErrorMessage(null);
            notificationRepository.save(notification);
            processedEventRepository.save(new ProcessedEvent(eventId));
            log.info("Sent {} notification for order {} to {}", type, orderId, email);
        } catch (RuntimeException ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(ex.getMessage());
            notificationRepository.save(notification);
            throw ex;
        }
    }
}
