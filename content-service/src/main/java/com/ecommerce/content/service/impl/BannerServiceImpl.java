package com.ecommerce.content.service.impl;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.content.dto.BannerRequest;
import com.ecommerce.content.dto.BannerResponse;
import com.ecommerce.content.entity.Banner;
import com.ecommerce.content.repository.BannerRepository;
import com.ecommerce.content.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findActiveBanners(Instant.now()).stream()
                .map(BannerResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public BannerResponse createBanner(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .linkUrl(request.getLinkUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        return BannerResponse.from(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public BannerResponse updateBanner(UUID id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner", "id", id));
        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setLinkUrl(request.getLinkUrl());
        banner.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        banner.setIsActive(request.getIsActive() == null || request.getIsActive());
        banner.setStartDate(request.getStartDate());
        banner.setEndDate(request.getEndDate());
        return BannerResponse.from(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public void deleteBanner(UUID id) {
        if (!bannerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Banner", "id", id);
        }
        bannerRepository.deleteById(id);
    }
}
