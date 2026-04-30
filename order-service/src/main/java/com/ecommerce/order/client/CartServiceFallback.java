package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CartServiceFallback implements CartServiceClient {

    @Override
    public ApiResponse<Void> clearCart(String userId) {
        log.warn("Cart Service unavailable; cart not cleared for user {}", userId);
        return ApiResponse.ok(null);
    }
}
