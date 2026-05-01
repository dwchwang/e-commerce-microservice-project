package com.ecommerce.payment.service;

import com.ecommerce.common.event.PaymentRequestedEvent;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.VnPayCreateResponse;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    VnPayCreateResponse createVnPayPayment(UUID orderId, String userId, String ipAddress);

    Map<String, String> handleVnPayIpn(Map<String, String> rawParams);

    String buildReturnRedirect(Map<String, String> rawParams);

    PaymentResponse getPayment(UUID orderId, String userId, String roles);

    void completeCodPayment(PaymentRequestedEvent event);

    void timeoutExpiredPayments();
}
