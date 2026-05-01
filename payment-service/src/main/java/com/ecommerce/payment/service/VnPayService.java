package com.ecommerce.payment.service;

import com.ecommerce.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public interface VnPayService {

    String createPaymentUrl(Payment payment, String ipAddress, LocalDateTime expiresAt);

    boolean verifySignature(Map<String, String> params);

    boolean isAmountMatching(String vnpAmount, BigDecimal amount);
}
