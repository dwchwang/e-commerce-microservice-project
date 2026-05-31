package com.ecommerce.flashsale.service.impl;

import com.ecommerce.common.event.FlashSaleOrderRequestedEvent;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.flashsale.dto.CampaignResponse;
import com.ecommerce.flashsale.dto.CreateCampaignRequest;
import com.ecommerce.flashsale.dto.PurchaseRequest;
import com.ecommerce.flashsale.dto.PurchaseResponse;
import com.ecommerce.flashsale.entity.CampaignStatus;
import com.ecommerce.flashsale.entity.FlashSaleCampaign;
import com.ecommerce.flashsale.exception.SlotReservationException;
import com.ecommerce.flashsale.kafka.FlashSaleEventProducer;
import com.ecommerce.flashsale.repository.FlashSaleCampaignRepository;
import com.ecommerce.flashsale.service.FlashSaleService;
import com.ecommerce.flashsale.util.FlashSaleRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl implements FlashSaleService {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
              return -2
            end
            local stock = redis.call('GET', KEYS[1])
            if not stock then
              return -3
            end
            stock = tonumber(stock)
            if stock <= 0 then
              return -1
            end
            local remaining = redis.call('DECR', KEYS[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            redis.call('EXPIRE', KEYS[2], ARGV[2])
            return remaining
            """, Long.class);

    private static final DefaultRedisScript<Long> COMPENSATE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('INCR', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[1])
            return 1
            """, Long.class);

    private final FlashSaleCampaignRepository campaignRepository;
    private final StringRedisTemplate redisTemplate;
    private final FlashSaleEventProducer eventProducer;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> getActiveCampaigns() {
        return campaignRepository.findActiveCampaigns(LocalDateTime.now()).stream()
                .map(campaign -> CampaignResponse.from(campaign, readRemainingStock(campaign)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> getAllCampaigns() {
        return campaignRepository.findAll(Sort.by(Sort.Direction.DESC, "startTime")).stream()
                .map(campaign -> CampaignResponse.from(campaign, readRemainingStock(campaign)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(UUID campaignId) {
        FlashSaleCampaign campaign = findCampaign(campaignId);
        return CampaignResponse.from(campaign, readRemainingStock(campaign));
    }

    @Override
    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        validateCampaignWindow(request);
        FlashSaleCampaign campaign = FlashSaleCampaign.builder()
                .id(UUID.randomUUID())
                .productId(request.getProductId())
                .sku(request.getSku())
                .productName(request.getProductName())
                .originalPrice(request.getOriginalPrice())
                .salePrice(request.getSalePrice())
                .quantity(request.getQuantity())
                .soldCount(0)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(CampaignStatus.SCHEDULED)
                .build();
        FlashSaleCampaign saved = campaignRepository.save(campaign);
        return CampaignResponse.from(saved, readRemainingStock(saved));
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(UUID campaignId, CreateCampaignRequest request) {
        validateCampaignWindow(request);
        FlashSaleCampaign campaign = findCampaign(campaignId);
        if (campaign.getStatus() != CampaignStatus.SCHEDULED) {
            throw new BusinessException("Only scheduled campaigns can be updated");
        }

        campaign.setProductId(request.getProductId());
        campaign.setSku(request.getSku());
        campaign.setProductName(request.getProductName());
        campaign.setOriginalPrice(request.getOriginalPrice());
        campaign.setSalePrice(request.getSalePrice());
        campaign.setQuantity(request.getQuantity());
        campaign.setStartTime(request.getStartTime());
        campaign.setEndTime(request.getEndTime());
        FlashSaleCampaign saved = campaignRepository.save(campaign);
        return CampaignResponse.from(saved, readRemainingStock(saved));
    }

    @Override
    @Transactional
    public void cancelCampaign(UUID campaignId) {
        FlashSaleCampaign campaign = findCampaign(campaignId);
        if (campaign.getStatus() == CampaignStatus.ENDED || campaign.getStatus() == CampaignStatus.CANCELLED) {
            return;
        }
        campaign.setStatus(CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
        redisTemplate.delete(List.of(FlashSaleRedisKeys.stockKey(campaignId), FlashSaleRedisKeys.buyersKey(campaignId)));
    }

    @Override
    public PurchaseResponse purchase(UUID campaignId, String userId, String userEmail, PurchaseRequest request) {
        FlashSaleCampaign campaign = findCampaign(campaignId);
        validateActiveWindow(campaign);

        long ttlSeconds = secondsUntil(campaign.getEndTime());
        Long result = redisTemplate.execute(RESERVE_SCRIPT,
                List.of(FlashSaleRedisKeys.stockKey(campaignId), FlashSaleRedisKeys.buyersKey(campaignId)),
                userId, String.valueOf(ttlSeconds));
        if (result == null) {
            throw new SlotReservationException("Cannot reserve flash sale slot");
        }
        if (result == -2L) {
            throw new BusinessException("You already purchased this campaign");
        }
        if (result == -1L) {
            throw new BusinessException("Sold out");
        }
        if (result == -3L) {
            throw new BusinessException("Campaign stock is not ready");
        }
        if (!LocalDateTime.now().isBefore(campaign.getEndTime())) {
            compensateReservedSlot(campaignId, userId);
            throw new BusinessException("Campaign is not active");
        }

        FlashSaleOrderRequestedEvent event = FlashSaleOrderRequestedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .userEmail(userEmail)
                .productId(campaign.getProductId())
                .sku(campaign.getSku())
                .productName(campaign.getProductName())
                .salePrice(campaign.getSalePrice())
                .flashSaleId(campaign.getId())
                .paymentMethod(request.getPaymentMethod().name())
                .shippingName(request.getShippingName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddress(request.getShippingAddress())
                .build();

        try {
            eventProducer.publishSync(event);
        } catch (Exception ex) {
            compensateReservedSlot(campaignId, userId);
            log.error("Flash sale purchase failed after reservation. campaign={}, user={}", campaignId, userId, ex);
            throw new SlotReservationException("Cannot create flash sale order. Please retry.");
        }

        try {
            int updated = transactionTemplate.execute(status -> campaignRepository.incrementSoldCount(campaignId));
            if (updated != 1) {
                throw new IllegalStateException("No campaign row updated");
            }
        } catch (Exception ex) {
            log.error("Kafka ACK succeeded but soldCount increment failed for campaign {}; reconciliation required",
                    campaignId, ex);
        }

        return PurchaseResponse.success(campaignId, result.intValue());
    }

    private FlashSaleCampaign findCampaign(UUID campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", "id", campaignId));
    }

    private void validateCampaignWindow(CreateCampaignRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("Campaign end time must be after start time");
        }
        if (!request.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Campaign start time must be in the future");
        }
        if (request.getOriginalPrice() != null
                && request.getSalePrice().compareTo(request.getOriginalPrice()) >= 0) {
            throw new BusinessException("Sale price must be less than original price");
        }
    }

    private void validateActiveWindow(FlashSaleCampaign campaign) {
        LocalDateTime now = LocalDateTime.now();
        if (campaign.getStatus() != CampaignStatus.ACTIVE
                || now.isBefore(campaign.getStartTime())
                || !now.isBefore(campaign.getEndTime())) {
            throw new BusinessException("Campaign is not active");
        }
    }

    private Integer readRemainingStock(FlashSaleCampaign campaign) {
        String value = redisTemplate.opsForValue().get(FlashSaleRedisKeys.stockKey(campaign.getId()));
        if (value == null) {
            return Math.max(campaign.getQuantity() - campaign.getSoldCount(), 0);
        }
        return Integer.valueOf(value);
    }

    private void compensateReservedSlot(UUID campaignId, String userId) {
        redisTemplate.execute(COMPENSATE_SCRIPT,
                List.of(FlashSaleRedisKeys.stockKey(campaignId), FlashSaleRedisKeys.buyersKey(campaignId)),
                userId);
    }

    private long secondsUntil(LocalDateTime endTime) {
        long seconds = Duration.between(LocalDateTime.now(), endTime).toSeconds();
        return Math.max(seconds, 1);
    }

}
