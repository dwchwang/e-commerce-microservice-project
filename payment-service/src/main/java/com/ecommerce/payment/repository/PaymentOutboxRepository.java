package com.ecommerce.payment.repository;

import com.ecommerce.payment.entity.PaymentOutboxEvent;
import com.ecommerce.payment.entity.PaymentOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxEvent, UUID> {

    @Query(value = """
            SELECT * FROM payment_outbox
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PaymentOutboxEvent> findPendingBatch();

    long countByAggregateIdAndEventType(String aggregateId, String eventType);

    List<PaymentOutboxEvent> findByStatus(PaymentOutboxStatus status);
}
