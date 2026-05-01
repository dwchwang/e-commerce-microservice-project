package com.ecommerce.flashsale.repository;

import com.ecommerce.flashsale.entity.CampaignStatus;
import com.ecommerce.flashsale.entity.FlashSaleCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FlashSaleCampaignRepository extends JpaRepository<FlashSaleCampaign, UUID> {

    List<FlashSaleCampaign> findByStatus(CampaignStatus status);

    List<FlashSaleCampaign> findByStatusAndStartTimeLessThanEqualAndEndTimeAfter(
            CampaignStatus status, LocalDateTime startTime, LocalDateTime endTime);

    List<FlashSaleCampaign> findByStatusAndEndTimeLessThanEqual(CampaignStatus status, LocalDateTime endTime);

    @Query("""
            SELECT c
            FROM FlashSaleCampaign c
            WHERE c.status = com.ecommerce.flashsale.entity.CampaignStatus.ACTIVE
              AND c.endTime > :now
            ORDER BY c.endTime ASC
            """)
    List<FlashSaleCampaign> findActiveCampaigns(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FlashSaleCampaign c SET c.soldCount = c.soldCount + 1 WHERE c.id = :id")
    int incrementSoldCount(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FlashSaleCampaign c SET c.soldCount = c.soldCount - :delta WHERE c.id = :id AND c.soldCount >= :delta")
    int decrementSoldCount(@Param("id") UUID id, @Param("delta") int delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FlashSaleCampaign c SET c.soldCount = :actual WHERE c.id = :id")
    int setSoldCount(@Param("id") UUID id, @Param("actual") int actual);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FlashSaleCampaign c
            SET c.status = :nextStatus
            WHERE c.id = :id AND c.status = :expectedStatus
            """)
    int updateStatusIfCurrent(@Param("id") UUID id,
                              @Param("expectedStatus") CampaignStatus expectedStatus,
                              @Param("nextStatus") CampaignStatus nextStatus);
}
