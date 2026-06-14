package com.ecommerce.payment.service.impl;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.event.PaymentFailedEvent;
import com.ecommerce.common.event.PaymentRequestedEvent;
import com.ecommerce.common.event.PaymentSuccessEvent;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.payment.client.OrderServiceClient;
import com.ecommerce.payment.config.VnPayConfig;
import com.ecommerce.payment.dto.OrderPaymentContextResponse;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.dto.VnPayCreateResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentOutboxEvent;
import com.ecommerce.payment.entity.PaymentOutboxStatus;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.entity.ProcessedEvent;
import com.ecommerce.payment.repository.PaymentOutboxRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.ProcessedEventRepository;
import com.ecommerce.payment.service.PaymentService;
import com.ecommerce.payment.service.VnPayService;
import com.ecommerce.payment.util.VnPayUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String VNPAY = "VNPAY";
    private static final String COD = "COD";

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final PaymentOutboxRepository outboxRepository;
    private final OrderServiceClient orderServiceClient;
    private final VnPayService vnPayService;
    private final VnPayConfig vnPayConfig;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public VnPayCreateResponse createVnPayPayment(UUID orderId, String userId, String ipAddress) {
        OrderPaymentContextResponse ctx = requireOrderContext(orderId);
        validateCreateContext(ctx, userId);

        Payment existing = paymentRepository.findByOrderIdAndStatusAndPaymentMethod(orderId, PaymentStatus.PENDING, VNPAY)
                .orElse(null);
        if (existing != null) {
            if (existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(LocalDateTime.now())) {
                return VnPayCreateResponse.builder()
                        .paymentId(existing.getId())
                        .orderId(existing.getOrderId())
                        .paymentUrl(existing.getPaymentUrl())
                        .expiresAt(existing.getExpiresAt())
                        .build();
            }
            markTerminal(existing, PaymentStatus.TIMEOUT, "Payment timeout at reservation expiry");
            throw new BusinessException("Order reservation has expired");
        }

        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(ctx.getUserId())
                .userEmail(ctx.getUserEmail())
                .paymentMethod(VNPAY)
                .amount(ctx.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .provider(VNPAY)
                .providerTxnRef(paymentId.toString())
                .expiresAt(ctx.getReservationExpiredAt())
                .build();
        payment.setPaymentUrl(vnPayService.createPaymentUrl(payment, ipAddress, ctx.getReservationExpiredAt()));
        Payment saved = paymentRepository.save(payment);
        log.info("Created VNPay payment {} for order {}", saved.getId(), orderId);
        return VnPayCreateResponse.builder()
                .paymentId(saved.getId())
                .orderId(saved.getOrderId())
                .paymentUrl(saved.getPaymentUrl())
                .expiresAt(saved.getExpiresAt())
                .build();
    }

    @Override
    public Map<String, String> handleVnPayIpn(Map<String, String> rawParams) {
        Map<String, String> params = VnPayUtil.filterVnPayParams(rawParams);
        if (!vnPayService.verifySignature(params)) {
            return ipn("97", "Invalid signature");
        }

        UUID paymentId;
        try {
            paymentId = UUID.fromString(params.getOrDefault("vnp_TxnRef", ""));
        } catch (IllegalArgumentException ex) {
            return ipn("01", "Order not found");
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            return ipn("01", "Order not found");
        }
        if (!vnPayConfig.getTmnCode().equals(params.get("vnp_TmnCode"))) {
            return ipn("97", "Invalid signature");
        }
        if (!vnPayService.isAmountMatching(params.get("vnp_Amount"), payment.getAmount())) {
            return ipn("04", "Invalid amount");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return ipn("02", "Already processed");
        }

        OrderPaymentContextResponse ctx;
        try {
            ctx = requireOrderContext(payment.getOrderId());
        } catch (RuntimeException ex) {
            log.warn("Order context unavailable during VNPay IPN for payment {}: {}", paymentId, ex.getMessage());
            return ipn("99", "Unknown error");
        }
        return transactionTemplate.execute(status -> processVnPayIpnInTransaction(paymentId, params, ctx));
    }

    private Map<String, String> processVnPayIpnInTransaction(
            UUID paymentId,
            Map<String, String> params,
            OrderPaymentContextResponse ctx) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            return ipn("01", "Order not found");
        }
        if (!vnPayConfig.getTmnCode().equals(params.get("vnp_TmnCode"))) {
            return ipn("97", "Invalid signature");
        }
        if (!vnPayService.isAmountMatching(params.get("vnp_Amount"), payment.getAmount())) {
            return ipn("04", "Invalid amount");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return ipn("02", "Already processed");
        }
        if (!"STOCK_RESERVED".equals(ctx.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailReason("Stale VNPay IPN for order status " + ctx.getStatus());
            saveVnPayFields(payment, params);
            paymentRepository.save(payment);
            return ipn("02", "Already processed");
        }

        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));
        saveVnPayFields(payment, params);
        if (success) {
            markTerminal(payment, PaymentStatus.COMPLETED, null);
            return ipn("00", "Confirm Success");
        }
        markTerminal(payment, PaymentStatus.FAILED, "VNPay response "
                + params.get("vnp_ResponseCode") + "/" + params.get("vnp_TransactionStatus"));
        return ipn("00", "Confirm Success");
    }

    @Override
    @Transactional(readOnly = true)
    public String buildReturnRedirect(Map<String, String> rawParams) {
        Map<String, String> params = VnPayUtil.filterVnPayParams(rawParams);
        boolean validSignature = vnPayService.verifySignature(params);
        UUID paymentId = parseUuid(params.get("vnp_TxnRef"));
        Payment payment = paymentId == null ? null : paymentRepository.findById(paymentId).orElse(null);
        String orderId = payment == null ? "unknown" : payment.getOrderId().toString();
        String status = validSignature && "00".equals(params.get("vnp_ResponseCode")) ? "success" : "failed";
        return UriComponentsBuilder.fromUriString(vnPayConfig.getFrontendResultUrl())
                .queryParam("orderId", orderId)
                .queryParam("paymentId", paymentId == null ? "unknown" : paymentId)
                .queryParam("status", status)
                .build()
                .toUriString();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID orderId, String userId, String roles) {
        Payment payment = paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
        if (!payment.getUserId().equals(userId) && (roles == null || !roles.contains("ADMIN"))) {
            throw new BusinessException("Payment does not belong to caller");
        }
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional
    public void completeCodPayment(PaymentRequestedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }
        if (!COD.equals(event.getPaymentMethod())) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId()));
            return;
        }
        if (event.getUserId() == null || event.getUserId().isBlank() || event.getAmount() == null) {
            throw new IllegalArgumentException("Invalid payment-requested payload");
        }
        if (paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(event.getOrderId()).isEmpty()) {
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .userEmail(event.getUserEmail())
                    .paymentMethod(COD)
                    .amount(event.getAmount())
                    .status(PaymentStatus.COMPLETED)
                    .provider(COD)
                    .build();
            int inserted = insertCompletedCodPayment(payment);
            if (inserted > 0) {
                saveSuccessOutbox(payment);
                meterRegistry.counter("ecommerce.payments", "method", COD, "result", "success").increment();
                log.info("Completed COD payment {} for order {}", payment.getId(), payment.getOrderId());
            } else {
                log.info("COD payment already exists for order {}", payment.getOrderId());
            }
        }
        processedEventRepository.save(new ProcessedEvent(event.getEventId()));
    }

    private int insertCompletedCodPayment(Payment payment) {
        try {
            return paymentRepository.insertCompletedCodPaymentIfAbsent(
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getUserEmail(),
                    payment.getAmount());
        } catch (DataIntegrityViolationException ex) {
            log.info("COD payment insert treated as idempotent for order {}", payment.getOrderId());
            return 0;
        }
    }

    @Override
    @Transactional
    public void timeoutExpiredPayments() {
        paymentRepository.findTop100ByStatusAndPaymentMethodAndExpiresAtBeforeOrderByExpiresAtAsc(
                        PaymentStatus.PENDING, VNPAY, LocalDateTime.now())
                .forEach(payment -> markTerminal(payment, PaymentStatus.TIMEOUT,
                        "Payment timeout at reservation expiry"));
    }

    private void validateCreateContext(OrderPaymentContextResponse ctx, String userId) {
        if (!ctx.getUserId().equals(userId)) {
            throw new BusinessException("Order does not belong to caller");
        }
        if (!"STOCK_RESERVED".equals(ctx.getStatus())) {
            throw new BusinessException("Order is not ready for VNPay payment");
        }
        if (!VNPAY.equals(ctx.getPaymentMethod())) {
            throw new BusinessException("Order payment method is not VNPay");
        }
        if (ctx.getReservationExpiredAt() == null || !ctx.getReservationExpiredAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Order reservation has expired");
        }
    }

    private OrderPaymentContextResponse requireOrderContext(UUID orderId) {
        ApiResponse<OrderPaymentContextResponse> response = orderServiceClient.getPaymentContext(orderId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BusinessException(response != null ? response.getMessage() : "Order Service unavailable");
        }
        return response.getData();
    }

    private void markTerminal(Payment payment, PaymentStatus status, String reason) {
        payment.setStatus(status);
        payment.setFailReason(reason);
        paymentRepository.save(payment);
        if (status == PaymentStatus.COMPLETED) {
            saveSuccessOutbox(payment);
        } else {
            saveFailureOutbox(payment, reason);
        }
        meterRegistry.counter("ecommerce.payments",
                "method", payment.getPaymentMethod(),
                "result", status == PaymentStatus.COMPLETED ? "success" : "failed").increment();
    }

    private void saveSuccessOutbox(Payment payment) {
        saveOutbox("PAYMENT_SUCCESS", "payment-success", PaymentSuccessEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(payment.getOrderId())
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .userId(payment.getUserId())
                .userEmail(payment.getUserEmail())
                .build(), payment);
    }

    private void saveFailureOutbox(Payment payment, String reason) {
        saveOutbox("PAYMENT_FAILED", "payment-failed", PaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(payment.getOrderId())
                .reason(reason)
                .userId(payment.getUserId())
                .userEmail(payment.getUserEmail())
                .build(), payment);
    }

    private void saveOutbox(String eventType, String topic, Object payload, Payment payment) {
        try {
            outboxRepository.save(PaymentOutboxEvent.builder()
                    .aggregateId(payment.getOrderId().toString())
                    .eventType(eventType)
                    .topic(topic)
                    .payload(objectMapper.writeValueAsString(payload))
                    .status(PaymentOutboxStatus.PENDING)
                    .attempts(0)
                    .build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize payment outbox event", ex);
        }
    }

    private void saveVnPayFields(Payment payment, Map<String, String> params) {
        payment.setVnpayTransactionNo(params.get("vnp_TransactionNo"));
        payment.setVnpayBankCode(params.get("vnp_BankCode"));
        payment.setVnpayPayDate(params.get("vnp_PayDate"));
        payment.setVnpayResponseCode(params.get("vnp_ResponseCode"));
        payment.setVnpayTransactionStatus(params.get("vnp_TransactionStatus"));
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Map<String, String> ipn(String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }
}
