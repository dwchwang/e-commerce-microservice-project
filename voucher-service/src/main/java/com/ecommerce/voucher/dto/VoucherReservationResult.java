package com.ecommerce.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherReservationResult {

    private boolean reserved;
    private BigDecimal discountAmount;
    private UUID reservationId;
    private String message;

    public static VoucherReservationResult reserved(BigDecimal discountAmount, UUID reservationId) {
        return VoucherReservationResult.builder()
                .reserved(true)
                .discountAmount(discountAmount)
                .reservationId(reservationId)
                .message("Voucher reserved")
                .build();
    }

    public static VoucherReservationResult alreadyReserved(BigDecimal discountAmount, UUID reservationId) {
        return VoucherReservationResult.builder()
                .reserved(true)
                .discountAmount(discountAmount)
                .reservationId(reservationId)
                .message("Voucher already reserved")
                .build();
    }
}
