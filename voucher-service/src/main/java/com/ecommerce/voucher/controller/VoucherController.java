package com.ecommerce.voucher.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.voucher.dto.VoucherRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Page<VoucherResponse>> getVouchers(Pageable pageable) {
        return ApiResponse.ok(voucherService.getVouchers(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse<List<VoucherResponse>> getActiveVouchers() {
        return ApiResponse.ok(voucherService.getActiveVouchers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<VoucherResponse> getVoucher(@PathVariable UUID id) {
        return ApiResponse.ok(voucherService.getVoucher(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<VoucherResponse> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return ApiResponse.ok("Voucher created successfully", voucherService.createVoucher(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<VoucherResponse> updateVoucher(
            @PathVariable UUID id,
            @Valid @RequestBody VoucherRequest request) {
        return ApiResponse.ok(voucherService.updateVoucher(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Void> deactivateVoucher(@PathVariable UUID id) {
        voucherService.deactivateVoucher(id);
        return ApiResponse.ok("Voucher deactivated", null);
    }
}
