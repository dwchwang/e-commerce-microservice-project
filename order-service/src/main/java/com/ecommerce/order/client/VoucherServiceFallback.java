package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.VoucherReservationRequest;
import com.ecommerce.order.dto.VoucherReservationResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class VoucherServiceFallback implements VoucherServiceClient {

    @Override
    public ApiResponse<VoucherReservationResult> reserve(String code, UUID orderId, VoucherReservationRequest request) {
        return ApiResponse.error("Voucher Service unavailable");
    }

    @Override
    public ApiResponse<Void> commit(String code, UUID orderId) {
        return ApiResponse.error("Voucher Service unavailable");
    }

    @Override
    public ApiResponse<Void> release(String code, UUID orderId) {
        return ApiResponse.error("Voucher Service unavailable");
    }
}
