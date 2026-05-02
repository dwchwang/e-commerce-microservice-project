package com.ecommerce.content.dto;

import com.ecommerce.content.entity.Banner;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BannerResponse {

    private UUID id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;
    private Instant updatedAt;

    public static BannerResponse from(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .displayOrder(banner.getDisplayOrder())
                .isActive(banner.getIsActive())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}
