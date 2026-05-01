package com.ecommerce.flashsale.scheduler;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.flashsale.client.OrderCountClient;
import com.ecommerce.flashsale.entity.CampaignStatus;
import com.ecommerce.flashsale.entity.FlashSaleCampaign;
import com.ecommerce.flashsale.repository.FlashSaleCampaignRepository;
import com.ecommerce.flashsale.util.FlashSaleRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private static final Duration ORPHAN_COMPENSATION_GRACE = Duration.ofSeconds(30);

    private final FlashSaleCampaignRepository campaignRepository;
    private final OrderCountClient orderCountClient;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 300_000)
    public void reconcileFlashSaleSlots() {
        List<FlashSaleCampaign> activeCampaigns = campaignRepository.findByStatus(CampaignStatus.ACTIVE);
        for (FlashSaleCampaign campaign : activeCampaigns) {
            reconcileCampaign(campaign);
        }
    }

    private void reconcileCampaign(FlashSaleCampaign campaign) {
        Long actualOrders = fetchActualOrderCount(campaign.getId());
        if (actualOrders == null) {
            return;
        }

        Integer redisRemaining = readRedisRemaining(campaign.getId());
        int dbSoldCount = campaign.getSoldCount();
        int actual = Math.toIntExact(actualOrders);

        if (dbSoldCount > actual) {
            if (LocalDateTime.now().isBefore(campaign.getStartTime().plus(ORPHAN_COMPENSATION_GRACE))) {
                return;
            }
            int orphanCount = dbSoldCount - actual;
            transactionTemplate.executeWithoutResult(status -> campaignRepository.setSoldCount(campaign.getId(), actual));
            if (redisRemaining != null) {
                redisTemplate.opsForValue().increment(FlashSaleRedisKeys.stockKey(campaign.getId()), orphanCount);
            }
            log.warn("Flash sale orphan slots compensated: campaign={}, orphans={}",
                    campaign.getId(), orphanCount);
            return;
        }

        if (actual > dbSoldCount) {
            transactionTemplate.executeWithoutResult(status -> campaignRepository.setSoldCount(campaign.getId(), actual));
            log.warn("Flash sale soldCount caught up from actual orders: campaign={}, from={}, to={}",
                    campaign.getId(), dbSoldCount, actual);
        }

        int expectedRemaining = campaign.getQuantity() - Math.max(dbSoldCount, actual);
        if (redisRemaining != null && redisRemaining != expectedRemaining) {
            log.warn("Redis/DB flash sale counter mismatch: campaign={}, redis={}, expected={}",
                    campaign.getId(), redisRemaining, expectedRemaining);
        }
    }

    private Long fetchActualOrderCount(UUID campaignId) {
        try {
            ApiResponse<Long> response = orderCountClient.countByFlashSaleId(campaignId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.warn("Could not fetch flash sale order count for campaign {}: {}",
                        campaignId, response != null ? response.getMessage() : "No response");
                return null;
            }
            return response.getData();
        } catch (Exception ex) {
            log.warn("Order count reconciliation skipped for campaign {}", campaignId, ex);
            return null;
        }
    }

    private Integer readRedisRemaining(UUID campaignId) {
        String value = redisTemplate.opsForValue().get(FlashSaleRedisKeys.stockKey(campaignId));
        return value == null ? null : Integer.valueOf(value);
    }
}
