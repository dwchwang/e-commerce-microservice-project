package com.ecommerce.flashsale.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderCountClientFallback implements OrderCountClient {

    @Override
    public ApiResponse<Long> countByFlashSaleId(UUID flashSaleId) {
        return ApiResponse.error("Order Service unavailable");
    }
}
