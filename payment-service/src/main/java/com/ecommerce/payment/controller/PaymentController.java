package com.ecommerce.payment.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.VnPayCreateResponse;
import com.ecommerce.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/vnpay/create")
    public ApiResponse<VnPayCreateResponse> createVnPayPayment(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam UUID orderId,
            HttpServletRequest request) {
        return ApiResponse.ok(paymentService.createVnPayPayment(orderId, userId, clientIp(request)));
    }

    @PostMapping("/vnpay/ipn")
    public Map<String, String> handleIpn(@RequestParam Map<String, String> params) {
        return paymentService.handleVnPayIpn(params);
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> handleReturn(@RequestParam Map<String, String> params) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(paymentService.buildReturnRedirect(params)))
                .build();
    }

    @GetMapping("/{orderId}")
    public ApiResponse<PaymentResponse> getPayment(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable UUID orderId) {
        return ApiResponse.ok(paymentService.getPayment(orderId, userId, roles));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
