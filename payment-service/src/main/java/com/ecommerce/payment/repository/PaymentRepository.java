package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findByOrderIdAndStatusAndPaymentMethod(UUID orderId, PaymentStatus status, String paymentMethod);

    Optional<Payment> findByOrderIdAndUserId(UUID orderId, String userId);

    List<Payment> findTop100ByStatusAndPaymentMethodAndExpiresAtBeforeOrderByExpiresAtAsc(
            PaymentStatus status,
            String paymentMethod,
            LocalDateTime expiresAt);

    @Modifying
    @Query(value = """
            INSERT INTO payments (
                id, order_id, user_id, user_email, payment_method, amount, status, provider, created_at, updated_at
            )
            VALUES (
                :id, :orderId, :userId, :userEmail, 'COD', :amount, 'COMPLETED', 'COD', NOW(), NOW()
            )
            ON CONFLICT (order_id) WHERE status = 'COMPLETED' AND payment_method = 'COD' DO NOTHING
            """, nativeQuery = true)
    int insertCompletedCodPaymentIfAbsent(
            @Param("id") UUID id,
            @Param("orderId") UUID orderId,
            @Param("userId") String userId,
            @Param("userEmail") String userEmail,
            @Param("amount") BigDecimal amount);
}
