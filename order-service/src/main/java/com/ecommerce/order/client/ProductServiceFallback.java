package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.ProductResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductServiceFallback implements ProductServiceClient {

    @Override
    public ApiResponse<ProductResponse> getProduct(UUID id) {
        return ApiResponse.error("Product Service unavailable");
    }
}
