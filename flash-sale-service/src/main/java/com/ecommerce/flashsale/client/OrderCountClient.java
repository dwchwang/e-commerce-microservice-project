package com.ecommerce.flashsale.client;

import com.ecommerce.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service", fallback = OrderCountClientFallback.class)
public interface OrderCountClient {

    @GetMapping("/internal/orders/count-by-flash-sale/{flashSaleId}")
    ApiResponse<Long> countByFlashSaleId(@PathVariable("flashSaleId") UUID flashSaleId);
}
