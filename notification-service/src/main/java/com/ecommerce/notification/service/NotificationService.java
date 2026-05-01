package com.ecommerce.notification.service;

import com.ecommerce.common.event.OrderCancelledEvent;
import com.ecommerce.common.event.OrderConfirmedEvent;
import com.ecommerce.notification.entity.Notification;

import java.util.List;

public interface NotificationService {

    void sendOrderConfirmed(OrderConfirmedEvent event);

    void sendOrderCancelled(OrderCancelledEvent event);

    List<Notification> findByReferenceId(String referenceId);
}
