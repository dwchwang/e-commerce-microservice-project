package com.ecommerce.content.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.content.dto.BannerRequest;
import com.ecommerce.content.dto.BannerResponse;
import com.ecommerce.content.service.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/active")
    public ApiResponse<List<BannerResponse>> getActiveBanners() {
        return ApiResponse.ok(bannerService.getActiveBanners());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BannerResponse> createBanner(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok("Banner created successfully", bannerService.createBanner(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BannerResponse> updateBanner(
            @PathVariable UUID id,
            @Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok(bannerService.updateBanner(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Void> deleteBanner(@PathVariable UUID id) {
        bannerService.deleteBanner(id);
        return ApiResponse.ok("Banner deleted successfully", null);
    }
}
