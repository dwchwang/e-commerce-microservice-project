package com.ecommerce.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentContextResponse {

    private UUID orderId;
    private String userId;
    private String userEmail;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime reservationExpiredAt;
}
