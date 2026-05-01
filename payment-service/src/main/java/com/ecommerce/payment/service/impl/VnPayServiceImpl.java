package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.config.VnPayConfig;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.service.VnPayService;
import com.ecommerce.payment.util.VnPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VnPayServiceImpl implements VnPayService {

    private final VnPayConfig config;

    @Override
    public String createPaymentUrl(Payment payment, String ipAddress, LocalDateTime expiresAt) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", config.getTmnCode());
        params.put("vnp_Amount", String.valueOf(VnPayUtil.toVnPayAmount(payment.getAmount())));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getId().toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrderId());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", config.getReturnUrl());
        params.put("vnp_IpAddr", ipAddress == null || ipAddress.isBlank() ? "127.0.0.1" : ipAddress);
        params.put("vnp_CreateDate", VnPayUtil.formatDate(LocalDateTime.now()));
        params.put("vnp_ExpireDate", VnPayUtil.formatDate(expiresAt));
        params.put("vnp_SecureHash", VnPayUtil.hmacSha512(config.getHashSecret(), VnPayUtil.signingData(params)));
        return config.getPayUrl() + "?" + VnPayUtil.queryString(params);
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        String supplied = params.get("vnp_SecureHash");
        if (supplied == null || supplied.isBlank()) {
            return false;
        }
        String expected = VnPayUtil.hmacSha512(config.getHashSecret(), VnPayUtil.signingData(params));
        return expected.equalsIgnoreCase(supplied);
    }

    @Override
    public boolean isAmountMatching(String vnpAmount, java.math.BigDecimal amount) {
        try {
            return Long.parseLong(vnpAmount) == VnPayUtil.toVnPayAmount(amount);
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
