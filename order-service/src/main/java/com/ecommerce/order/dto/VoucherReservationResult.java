package com.ecommerce.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VoucherReservationResult {

    private boolean reserved;
    private BigDecimal discountAmount;
    private UUID reservationId;
    private String message;
}
