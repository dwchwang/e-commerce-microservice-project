package com.ecommerce.review.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "order-service", fallback = OrderServiceFallback.class)
public interface OrderServiceClient {

    @GetMapping("/api/orders/user/{userId}/product/{productId}/confirmed")
    ApiResponse<Map<String, Boolean>> hasConfirmedOrder(
            @PathVariable("userId") String userId,
            @PathVariable("productId") UUID productId);
}
