package com.ecommerce.flashsale.scheduler;

import com.ecommerce.flashsale.entity.CampaignStatus;
import com.ecommerce.flashsale.entity.FlashSaleCampaign;
import com.ecommerce.flashsale.repository.FlashSaleCampaignRepository;
import com.ecommerce.flashsale.util.FlashSaleRedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
public class CampaignScheduler {

    private static final String LOCK_KEY = "flash_sale:scheduler:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final FlashSaleCampaignRepository campaignRepository;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final String instanceId = UUID.randomUUID().toString();

    @Scheduled(fixedDelay = 5_000)
    public void manageCampaigns() {
        if (!tryAcquireSchedulerLock()) {
            return;
        }
        try {
            activateDueCampaigns();
            recoverActiveCampaignRedisKeys();
            endExpiredCampaigns();
        } finally {
            releaseSchedulerLockIfOwner();
        }
    }

    private void activateDueCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<FlashSaleCampaign> dueCampaigns = campaignRepository
                .findByStatusAndStartTimeLessThanEqualAndEndTimeAfter(CampaignStatus.SCHEDULED, now, now);
        for (FlashSaleCampaign campaign : dueCampaigns) {
            Integer updated = transactionTemplate.execute(status -> campaignRepository.updateStatusIfCurrent(
                    campaign.getId(), CampaignStatus.SCHEDULED, CampaignStatus.ACTIVE));
            if (updated != null && updated == 1) {
                seedRedisStock(campaign, campaign.getQuantity() - campaign.getSoldCount());
                log.info("Activated flash sale campaign {}", campaign.getId());
            }
        }
    }

    private void recoverActiveCampaignRedisKeys() {
        for (FlashSaleCampaign campaign : campaignRepository.findActiveCampaigns(LocalDateTime.now())) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(FlashSaleRedisKeys.stockKey(campaign.getId())))) {
                continue;
            }
            int remaining = Math.max(campaign.getQuantity() - campaign.getSoldCount(), 0);
            if (remaining > 0) {
                seedRedisStock(campaign, remaining);
                log.warn("Recovered missing Redis stock key for active flash sale campaign {}", campaign.getId());
            }
        }
    }

    private void endExpiredCampaigns() {
        List<FlashSaleCampaign> expiredCampaigns = campaignRepository
                .findByStatusAndEndTimeLessThanEqual(CampaignStatus.ACTIVE, LocalDateTime.now());
        for (FlashSaleCampaign campaign : expiredCampaigns) {
            Integer updated = transactionTemplate.execute(status -> campaignRepository.updateStatusIfCurrent(
                    campaign.getId(), CampaignStatus.ACTIVE, CampaignStatus.ENDED));
            if (updated != null && updated == 1) {
                log.info("Ended flash sale campaign {}", campaign.getId());
            }
        }
    }

    private void seedRedisStock(FlashSaleCampaign campaign, int remaining) {
        Duration ttl = ttlUntil(campaign.getEndTime());
        redisTemplate.opsForValue().set(
                FlashSaleRedisKeys.stockKey(campaign.getId()),
                String.valueOf(Math.max(remaining, 0)),
                ttl);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(FlashSaleRedisKeys.buyersKey(campaign.getId())))) {
            redisTemplate.expire(FlashSaleRedisKeys.buyersKey(campaign.getId()), ttl);
        }
    }

    private boolean tryAcquireSchedulerLock() {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, instanceId, LOCK_TTL));
    }

    private void releaseSchedulerLockIfOwner() {
        redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(LOCK_KEY), instanceId);
    }

    private Duration ttlUntil(LocalDateTime endTime) {
        long seconds = Duration.between(LocalDateTime.now(), endTime).toSeconds();
        return Duration.ofSeconds(Math.max(seconds, 1));
    }

}
