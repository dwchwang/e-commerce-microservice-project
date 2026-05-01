package com.ecommerce.payment.client;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.payment.dto.OrderPaymentContextResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service", fallback = OrderServiceClientFallback.class)
public interface OrderServiceClient {

    @GetMapping("/internal/orders/{orderId}/payment-context")
    ApiResponse<OrderPaymentContextResponse> getPaymentContext(@PathVariable("orderId") UUID orderId);
}
