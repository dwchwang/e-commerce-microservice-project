package com.ecommerce.payment.service.impl;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.event.PaymentRequestedEvent;
import com.ecommerce.payment.client.OrderServiceClient;
import com.ecommerce.payment.config.VnPayConfig;
import com.ecommerce.payment.dto.OrderPaymentContextResponse;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentOutboxEvent;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.entity.ProcessedEvent;
import com.ecommerce.payment.repository.PaymentOutboxRepository;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.repository.ProcessedEventRepository;
import com.ecommerce.payment.service.VnPayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final ProcessedEventRepository processedEventRepository = mock(ProcessedEventRepository.class);
    private final PaymentOutboxRepository outboxRepository = mock(PaymentOutboxRepository.class);
    private final OrderServiceClient orderServiceClient = mock(OrderServiceClient.class);
    private final VnPayService vnPayService = mock(VnPayService.class);
    private final VnPayConfig vnPayConfig = new VnPayConfig();
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(
            paymentRepository,
            processedEventRepository,
            outboxRepository,
            orderServiceClient,
            vnPayService,
            vnPayConfig,
            new ObjectMapper(),
            transactionTemplate,
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

    PaymentServiceImplTest() {
        vnPayConfig.setTmnCode("DEMO_TMN");
        vnPayConfig.setFrontendResultUrl("http://frontend/payment-result");
    }

    @Test
    void buildReturnRedirectDoesNotTrustSuccessfulResponseCodeWithoutValidSignature() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(vnPayService.verifySignature(any())).thenReturn(false);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment(paymentId, orderId)));

        String redirect = service.buildReturnRedirect(Map.of(
                "vnp_TxnRef", paymentId.toString(),
                "vnp_ResponseCode", "00",
                "vnp_SecureHash", "forged"));

        assertThat(redirect).contains("orderId=" + orderId);
        assertThat(redirect).contains("paymentId=" + paymentId);
        assertThat(redirect).contains("status=failed");
    }

    @Test
    void buildReturnRedirectMarksSuccessOnlyWhenSignatureIsValid() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(vnPayService.verifySignature(any())).thenReturn(true);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment(paymentId, orderId)));

        String redirect = service.buildReturnRedirect(Map.of(
                "vnp_TxnRef", paymentId.toString(),
                "vnp_ResponseCode", "00",
                "vnp_SecureHash", "valid"));

        assertThat(redirect).contains("status=success");
    }

    @Test
    void getPaymentUsesMostRecentPaymentForOrder() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment latest = payment(paymentId, orderId);
        latest.setUserId("user-1");
        when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.of(latest));

        PaymentResponse response = service.getPayment(orderId, "user-1", "");

        assertThat(response.getId()).isEqualTo(paymentId);
        verify(paymentRepository).findTopByOrderIdOrderByCreatedAtDesc(orderId);
    }

    @Test
    void completeCodPaymentUsesConflictSafeInsertAndPublishesOutboxOnlyWhenInserted() {
        PaymentRequestedEvent event = codEvent();
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(event.getOrderId())).thenReturn(Optional.empty());
        when(paymentRepository.insertCompletedCodPaymentIfAbsent(
                any(), any(), any(), any(), any())).thenReturn(1);

        service.completeCodPayment(event);

        verify(paymentRepository).insertCompletedCodPaymentIfAbsent(
                any(), any(), any(), any(), any());
        ArgumentCaptor<PaymentOutboxEvent> outboxCaptor = ArgumentCaptor.forClass(PaymentOutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getTopic()).isEqualTo("payment-success");
        assertThat(outboxCaptor.getValue().getAggregateId()).isEqualTo(event.getOrderId().toString());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void completeCodPaymentTreatsInsertConflictAsIdempotentAndDoesNotPublishDuplicateOutbox() {
        PaymentRequestedEvent event = codEvent();
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(paymentRepository.findTopByOrderIdOrderByCreatedAtDesc(event.getOrderId())).thenReturn(Optional.empty());
        when(paymentRepository.insertCompletedCodPaymentIfAbsent(
                any(), any(), any(), any(), any())).thenReturn(0);

        service.completeCodPayment(event);

        verify(outboxRepository, never()).save(any());
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void handleVnPayIpnFetchesOrderContextBeforeStartingWriteTransaction() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment pending = payment(paymentId, orderId);
        pending.setStatus(PaymentStatus.PENDING);
        pending.setAmount(BigDecimal.valueOf(100000));
        when(vnPayService.verifySignature(any())).thenReturn(true);
        when(vnPayService.isAmountMatching("10000000", BigDecimal.valueOf(100000))).thenReturn(true);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pending));
        when(orderServiceClient.getPaymentContext(orderId)).thenReturn(ApiResponse.ok(OrderPaymentContextResponse.builder()
                .orderId(orderId)
                .userId("user-1")
                .userEmail("user@example.com")
                .paymentMethod("VNPAY")
                .totalAmount(BigDecimal.valueOf(100000))
                .status("STOCK_RESERVED")
                .reservationExpiredAt(LocalDateTime.now().plusMinutes(10))
                .build()));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Map<String, String>> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        Map<String, String> response = service.handleVnPayIpn(Map.of(
                "vnp_TxnRef", paymentId.toString(),
                "vnp_TmnCode", "DEMO_TMN",
                "vnp_Amount", "10000000",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00",
                "vnp_SecureHash", "valid"));

        assertThat(response).containsEntry("RspCode", "00");
        assertThat(PaymentServiceImpl.class.getMethod("handleVnPayIpn", Map.class)
                .getAnnotation(Transactional.class)).isNull();
        var ordered = inOrder(orderServiceClient, transactionTemplate);
        ordered.verify(orderServiceClient).getPaymentContext(orderId);
        ordered.verify(transactionTemplate).execute(any());
    }

    private Payment payment(UUID paymentId, UUID orderId) {
        return Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId("user-1")
                .userEmail("user@example.com")
                .paymentMethod("VNPAY")
                .amount(BigDecimal.valueOf(100000))
                .status(PaymentStatus.COMPLETED)
                .provider("VNPAY")
                .build();
    }

    private PaymentRequestedEvent codEvent() {
        return PaymentRequestedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .userId("user-1")
                .userEmail("user@example.com")
                .amount(BigDecimal.valueOf(100000))
                .paymentMethod("COD")
                .build();
    }
}
