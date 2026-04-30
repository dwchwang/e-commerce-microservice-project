package com.ecommerce.voucher.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.voucher.dto.VoucherRequest;
import com.ecommerce.voucher.dto.VoucherReservationResult;
import com.ecommerce.voucher.dto.VoucherResponse;
import com.ecommerce.voucher.entity.DiscountType;
import com.ecommerce.voucher.entity.Voucher;
import com.ecommerce.voucher.entity.VoucherReservation;
import com.ecommerce.voucher.entity.VoucherReservationStatus;
import com.ecommerce.voucher.entity.VoucherUsage;
import com.ecommerce.voucher.repository.VoucherRepository;
import com.ecommerce.voucher.repository.VoucherReservationRepository;
import com.ecommerce.voucher.repository.VoucherUsageRepository;
import com.ecommerce.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final VoucherRepository voucherRepository;
    private final VoucherReservationRepository voucherReservationRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getVouchers(Pageable pageable) {
        return voucherRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getActiveVouchers() {
        LocalDateTime now = LocalDateTime.now();
        return voucherRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(now, now).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucher(UUID id) {
        return toResponse(findVoucher(id));
    }

    @Override
    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        String code = normalizeCode(request.getCode());
        if (voucherRepository.existsByCode(code)) {
            throw new BusinessException("Voucher code already exists");
        }
        validateVoucherRequest(request);

        Voucher voucher = Voucher.builder()
                .code(code)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(defaultZero(request.getMinOrderValue()))
                .maxDiscount(request.getMaxDiscount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();

        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(UUID id, VoucherRequest request) {
        Voucher voucher = findVoucher(id);
        String code = normalizeCode(request.getCode());
        voucherRepository.findByCode(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("Voucher code already exists");
                });
        validateVoucherRequest(request);

        voucher.setCode(code);
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinOrderValue(defaultZero(request.getMinOrderValue()));
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        if (request.getIsActive() != null) {
            voucher.setIsActive(request.getIsActive());
        }

        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public void deactivateVoucher(UUID id) {
        Voucher voucher = findVoucher(id);
        voucher.setIsActive(false);
        voucherRepository.save(voucher);
    }

    @Override
    @Transactional
    public VoucherReservationResult reserve(String code, UUID orderId, String userId, BigDecimal orderTotal) {
        Voucher voucher = voucherRepository.findByCodeWithLock(normalizeCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "code", code));

        Optional<VoucherReservation> existing = voucherReservationRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            VoucherReservation reservation = existing.get();
            return VoucherReservationResult.alreadyReserved(reservation.getDiscountAmount(), reservation.getId());
        }

        validateVoucherEligibility(voucher, orderTotal);

        if (voucher.getUsageLimit() != null) {
            long activeReservations = voucherReservationRepository
                    .countByVoucherIdAndStatusAndExpiresAtAfter(
                            voucher.getId(),
                            VoucherReservationStatus.RESERVED,
                            LocalDateTime.now());
            if (voucher.getUsedCount() + activeReservations >= voucher.getUsageLimit()) {
                throw new BusinessException("Voucher usage limit reached");
            }
        }

        BigDecimal discount = calculateDiscount(voucher, orderTotal);
        VoucherReservation reservation = VoucherReservation.builder()
                .voucherId(voucher.getId())
                .orderId(orderId)
                .userId(userId)
                .discountAmount(discount)
                .status(VoucherReservationStatus.RESERVED)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        VoucherReservation saved = voucherReservationRepository.save(reservation);
        return VoucherReservationResult.reserved(saved.getDiscountAmount(), saved.getId());
    }

    @Override
    @Transactional
    public void commit(String code, UUID orderId) {
        VoucherReservation reservation = voucherReservationRepository.findByOrderIdWithLock(orderId)
                .orElseThrow(() -> new BusinessException("No reservation for order: " + orderId));

        if (reservation.getStatus() == VoucherReservationStatus.COMMITTED) {
            return;
        }
        if (reservation.getStatus() != VoucherReservationStatus.RESERVED) {
            throw new BusinessException("No active reservation for order: " + orderId);
        }

        Voucher voucher = voucherRepository.findByIdWithLock(reservation.getVoucherId())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", reservation.getVoucherId()));
        ensureVoucherCodeMatches(voucher, code);

        reservation.setStatus(VoucherReservationStatus.COMMITTED);
        voucherReservationRepository.save(reservation);

        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);

        if (!voucherUsageRepository.existsByOrderId(orderId)) {
            voucherUsageRepository.save(VoucherUsage.builder()
                    .voucherId(voucher.getId())
                    .userId(reservation.getUserId())
                    .orderId(orderId)
                    .discountAmount(reservation.getDiscountAmount())
                    .build());
        }
    }

    @Override
    @Transactional
    public void release(String code, UUID orderId) {
        voucherReservationRepository.findByOrderIdAndStatus(orderId, VoucherReservationStatus.RESERVED)
                .ifPresent(reservation -> {
                    Voucher voucher = voucherRepository.findById(reservation.getVoucherId())
                            .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", reservation.getVoucherId()));
                    ensureVoucherCodeMatches(voucher, code);
                    reservation.setStatus(VoucherReservationStatus.RELEASED);
                    voucherReservationRepository.save(reservation);
                });
    }

    private void validateVoucherRequest(VoucherRequest request) {
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BusinessException("Voucher end date must be after start date");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(ONE_HUNDRED) > 0) {
            throw new BusinessException("Percentage discount cannot exceed 100");
        }
    }

    private void validateVoucherEligibility(Voucher voucher, BigDecimal orderTotal) {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new BusinessException("Voucher is inactive");
        }
        if (now.isBefore(voucher.getStartDate())) {
            throw new BusinessException("Voucher is not active yet");
        }
        if (now.isAfter(voucher.getEndDate())) {
            throw new BusinessException("Voucher expired");
        }
        if (orderTotal.compareTo(defaultZero(voucher.getMinOrderValue())) < 0) {
            throw new BusinessException("Order total does not meet voucher minimum");
        }
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderTotal.multiply(voucher.getDiscountValue())
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscount() != null && discount.compareTo(voucher.getMaxDiscount()) > 0) {
                discount = voucher.getMaxDiscount();
            }
        } else {
            discount = voucher.getDiscountValue();
        }

        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    private Voucher findVoucher(UUID id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", id));
    }

    private void ensureVoucherCodeMatches(Voucher voucher, String code) {
        if (!voucher.getCode().equals(normalizeCode(code))) {
            throw new BusinessException("Voucher code does not match reservation");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private VoucherResponse toResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minOrderValue(voucher.getMinOrderValue())
                .maxDiscount(voucher.getMaxDiscount())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .isActive(voucher.getIsActive())
                .createdAt(voucher.getCreatedAt())
                .updatedAt(voucher.getUpdatedAt())
                .build();
    }
}
