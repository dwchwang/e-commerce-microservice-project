package com.ecommerce.flashsale.dto;

import com.ecommerce.flashsale.entity.CampaignStatus;
import com.ecommerce.flashsale.entity.FlashSaleCampaign;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CampaignResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private String productName;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private Integer quantity;
    private Integer soldCount;
    private Integer remainingStock;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private CampaignStatus status;

    public static CampaignResponse from(FlashSaleCampaign campaign, Integer remainingStock) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .productId(campaign.getProductId())
                .sku(campaign.getSku())
                .productName(campaign.getProductName())
                .originalPrice(campaign.getOriginalPrice())
                .salePrice(campaign.getSalePrice())
                .quantity(campaign.getQuantity())
                .soldCount(campaign.getSoldCount())
                .remainingStock(remainingStock)
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .status(campaign.getStatus())
                .build();
    }
}
