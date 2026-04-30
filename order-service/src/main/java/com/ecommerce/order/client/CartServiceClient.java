package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", fallback = CartServiceFallback.class)
public interface CartServiceClient {

    @DeleteMapping("/internal/cart/{userId}")
    ApiResponse<Void> clearCart(@PathVariable("userId") String userId);
}
