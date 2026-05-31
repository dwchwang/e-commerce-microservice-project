package com.ecommerce.voucher.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.voucher.dto.VoucherReservationRequest;
import com.ecommerce.voucher.dto.VoucherReservationResult;
import com.ecommerce.voucher.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/vouchers")
@RequiredArgsConstructor
public class InternalVoucherController {

    private final VoucherService voucherService;

    @PostMapping("/{code}/reserve")
    public ApiResponse<VoucherReservationResult> reserve(
            @PathVariable String code,
            @RequestParam UUID orderId,
            @Valid @RequestBody VoucherReservationRequest request) {
        return ApiResponse.ok(voucherService.reserve(code, orderId, request.getUserId(), request.getOrderTotal()));
    }

    @PostMapping("/{code}/commit")
    public ApiResponse<Void> commit(@PathVariable String code, @RequestParam UUID orderId) {
        voucherService.commit(code, orderId);
        return ApiResponse.ok("Voucher committed", null);
    }

    @PostMapping("/{code}/release")
    public ApiResponse<Void> release(@PathVariable String code, @RequestParam UUID orderId) {
        voucherService.release(code, orderId);
        return ApiResponse.ok("Voucher released", null);
    }
}
