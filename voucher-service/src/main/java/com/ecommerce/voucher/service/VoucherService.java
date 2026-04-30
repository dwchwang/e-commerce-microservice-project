package com.ecommerce.voucher.service;

import com.ecommerce.voucher.dto.VoucherRequest;
import com.ecommerce.voucher.dto.VoucherReservationResult;
import com.ecommerce.voucher.dto.VoucherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface VoucherService {

    Page<VoucherResponse> getVouchers(Pageable pageable);

    List<VoucherResponse> getActiveVouchers();

    VoucherResponse getVoucher(UUID id);

    VoucherResponse createVoucher(VoucherRequest request);

    VoucherResponse updateVoucher(UUID id, VoucherRequest request);

    void deactivateVoucher(UUID id);

    VoucherReservationResult reserve(String code, UUID orderId, String userId, BigDecimal orderTotal);

    void commit(String code, UUID orderId);

    void release(String code, UUID orderId);
}
