package com.ecommerce.cart.client;

import com.ecommerce.cart.dto.ProductResponse;
import com.ecommerce.common.dto.ApiResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductServiceFallback implements ProductServiceClient {

    @Override
    public ApiResponse<ProductResponse> getProduct(UUID id) {
        return ApiResponse.error("Product Service unavailable");
    }
}
