package com.ecommerce.content.service;

import com.ecommerce.content.dto.BannerRequest;
import com.ecommerce.content.dto.BannerResponse;

import java.util.List;
import java.util.UUID;

public interface BannerService {

    List<BannerResponse> getActiveBanners();

    BannerResponse createBanner(BannerRequest request);

    BannerResponse updateBanner(UUID id, BannerRequest request);

    void deleteBanner(UUID id);
}
