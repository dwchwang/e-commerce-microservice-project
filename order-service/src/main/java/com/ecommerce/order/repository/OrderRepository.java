package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END
            FROM Order o JOIN o.items i
            WHERE o.userId = :userId
              AND i.productId = :productId
              AND o.status = com.ecommerce.order.entity.OrderStatus.CONFIRMED
            """)
    boolean existsConfirmedOrderForProduct(@Param("userId") String userId, @Param("productId") UUID productId);

    Optional<Order> findByIdAndUserId(UUID id, String userId);

    List<Order> findByStatusAndPaymentMethodAndReservationExpiredAtBefore(
            OrderStatus status,
            PaymentMethod paymentMethod,
            LocalDateTime reservationExpiredAt);
}
