package com.ecommerce.payment.client;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.payment.dto.OrderPaymentContextResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    public ApiResponse<OrderPaymentContextResponse> getPaymentContext(UUID orderId) {
        return ApiResponse.error("Order Service unavailable");
    }
}
