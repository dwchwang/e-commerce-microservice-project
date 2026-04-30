package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> placeOrder(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody PlaceOrderRequest request) {
        return ApiResponse.ok(orderService.placeOrder(userId, userEmail, request));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ApiResponse.ok(orderService.getUserOrder(userId, id));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<Map<String, String>> getOrderStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ApiResponse.ok(Map.of(
                "orderId", id.toString(),
                "status", orderService.getOrderStatus(userId, id)));
    }

    @GetMapping("/user/{userId}/product/{productId}/confirmed")
    public ApiResponse<Map<String, Boolean>> hasConfirmedOrder(
            @RequestHeader(value = "X-User-Id", required = false) String callerUserId,
            @PathVariable String userId,
            @PathVariable UUID productId) {
        if (callerUserId != null && !callerUserId.equals(userId)) {
            throw new BusinessException("Unauthorized");
        }
        return ApiResponse.ok(Map.of("confirmed", orderService.hasConfirmedOrderForProduct(userId, productId)));
    }
}
