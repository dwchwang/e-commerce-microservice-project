package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.dto.OrderPaymentContextResponse;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderRepository orderRepository;

    @GetMapping("/{orderId}/payment-context")
    public ApiResponse<OrderPaymentContextResponse> getPaymentContext(@PathVariable UUID orderId) {
        return ApiResponse.ok(orderRepository.findById(orderId)
                .map(OrderPaymentContextResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId)));
    }
}
