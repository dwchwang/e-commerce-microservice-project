package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private String userId;
    private String userEmail;
    private String paymentMethod;
    private BigDecimal amount;
    private String status;
    private String provider;
    private String paymentUrl;
    private LocalDateTime expiresAt;
    private String failReason;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .userEmail(payment.getUserEmail())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .provider(payment.getProvider())
                .paymentUrl(payment.getPaymentUrl())
                .expiresAt(payment.getExpiresAt())
                .failReason(payment.getFailReason())
                .build();
    }
}
