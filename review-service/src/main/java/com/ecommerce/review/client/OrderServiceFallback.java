package com.ecommerce.review.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderServiceFallback implements OrderServiceClient {

    @Override
    public ApiResponse<Map<String, Boolean>> hasConfirmedOrder(String userId, UUID productId) {
        return ApiResponse.ok(Map.of("confirmed", false));
    }
}
