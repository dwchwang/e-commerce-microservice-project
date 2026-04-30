package com.ecommerce.voucher.repository;

import com.ecommerce.voucher.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, UUID> {

    boolean existsByOrderId(UUID orderId);
}
