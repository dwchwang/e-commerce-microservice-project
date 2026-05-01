package com.ecommerce.notification.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/order/{orderId}")
    public ApiResponse<List<Notification>> getOrderNotifications(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String orderId) {
        return ApiResponse.ok(notificationService.findByReferenceId(orderId).stream()
                .filter(notification -> userId.equals(notification.getUserId()))
                .toList());
    }
}
