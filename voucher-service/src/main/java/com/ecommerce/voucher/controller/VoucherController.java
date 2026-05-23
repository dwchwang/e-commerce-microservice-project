package com.ecommerce.voucher.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.voucher.dto.VoucherRequest;
import com.ecommerce.voucher.dto.VoucherReservationRequest;
import com.ecommerce.voucher.dto.VoucherReservationResult;
import com.ecommerce.voucher.dto.VoucherResponse;
import com.ecommerce.voucher.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/api/vouchers")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Page<VoucherResponse>> getVouchers(Pageable pageable) {
        return ApiResponse.ok(voucherService.getVouchers(pageable));
    }

    @GetMapping("/api/vouchers/active")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse<List<VoucherResponse>> getActiveVouchers() {
        return ApiResponse.ok(voucherService.getActiveVouchers());
    }

    @GetMapping("/api/vouchers/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<VoucherResponse> getVoucher(@PathVariable UUID id) {
        return ApiResponse.ok(voucherService.getVoucher(id));
    }

    @PostMapping("/api/vouchers")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<VoucherResponse> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return ApiResponse.ok("Voucher created successfully", voucherService.createVoucher(request));
    }

    @PutMapping("/api/vouchers/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<VoucherResponse> updateVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody VoucherRequest request) {
        return ApiResponse.ok(voucherService.updateVoucher(id, request));
    }

    @DeleteMapping("/api/vouchers/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Void> deactivateVoucher(@PathVariable UUID id) {
        voucherService.deactivateVoucher(id);
        return ApiResponse.ok("Voucher deactivated", null);
    }

    @PostMapping("/internal/vouchers/{code}/reserve")
    public ApiResponse<VoucherReservationResult> reserve(
            @PathVariable String code,
            @RequestParam UUID orderId,
            @Valid @RequestBody VoucherReservationRequest request) {
        return ApiResponse.ok(voucherService.reserve(code, orderId, request.getUserId(), request.getOrderTotal()));
    }

    @PostMapping("/internal/vouchers/{code}/commit")
    public ApiResponse<Void> commit(@PathVariable String code, @RequestParam UUID orderId) {
        voucherService.commit(code, orderId);
        return ApiResponse.ok("Voucher committed", null);
    }

    @PostMapping("/internal/vouchers/{code}/release")
    public ApiResponse<Void> release(@PathVariable String code, @RequestParam UUID orderId) {
        voucherService.release(code, orderId);
        return ApiResponse.ok("Voucher released", null);
    }
}
