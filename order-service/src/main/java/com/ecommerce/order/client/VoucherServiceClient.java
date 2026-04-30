package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.dto.VoucherReservationRequest;
import com.ecommerce.order.dto.VoucherReservationResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "voucher-service", fallback = VoucherServiceFallback.class)
public interface VoucherServiceClient {

    @PostMapping("/internal/vouchers/{code}/reserve")
    ApiResponse<VoucherReservationResult> reserve(
            @PathVariable("code") String code,
            @RequestParam("orderId") UUID orderId,
            @RequestBody VoucherReservationRequest request);

    @PostMapping("/internal/vouchers/{code}/commit")
    ApiResponse<Void> commit(@PathVariable("code") String code, @RequestParam("orderId") UUID orderId);

    @PostMapping("/internal/vouchers/{code}/release")
    ApiResponse<Void> release(@PathVariable("code") String code, @RequestParam("orderId") UUID orderId);
}
