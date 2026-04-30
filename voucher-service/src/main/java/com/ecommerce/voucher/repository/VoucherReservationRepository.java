package com.ecommerce.voucher.repository;

import com.ecommerce.voucher.entity.VoucherReservation;
import com.ecommerce.voucher.entity.VoucherReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VoucherReservationRepository extends JpaRepository<VoucherReservation, UUID> {

    Optional<VoucherReservation> findByOrderId(UUID orderId);

    Optional<VoucherReservation> findByOrderIdAndStatus(UUID orderId, VoucherReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM VoucherReservation r WHERE r.orderId = :orderId")
    Optional<VoucherReservation> findByOrderIdWithLock(@Param("orderId") UUID orderId);

    long countByVoucherIdAndStatusAndExpiresAtAfter(
            UUID voucherId,
            VoucherReservationStatus status,
            LocalDateTime now);
}
