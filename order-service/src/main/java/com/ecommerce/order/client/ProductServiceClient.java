package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service", fallback = ProductServiceFallback.class)
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("id") UUID id);
}
