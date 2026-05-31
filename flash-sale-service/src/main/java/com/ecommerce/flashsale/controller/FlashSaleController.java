package com.ecommerce.flashsale.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.flashsale.dto.CampaignResponse;
import com.ecommerce.flashsale.dto.CreateCampaignRequest;
import com.ecommerce.flashsale.dto.PurchaseRequest;
import com.ecommerce.flashsale.dto.PurchaseResponse;
import com.ecommerce.flashsale.service.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping
    public ApiResponse<List<CampaignResponse>> getActiveCampaigns() {
        return ApiResponse.ok(flashSaleService.getActiveCampaigns());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<List<CampaignResponse>> getAllCampaigns() {
        return ApiResponse.ok(flashSaleService.getAllCampaigns());
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignResponse> getCampaign(@PathVariable UUID id) {
        return ApiResponse.ok(flashSaleService.getCampaign(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<CampaignResponse> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request) {
        return ApiResponse.ok("Campaign created", flashSaleService.createCampaign(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<CampaignResponse> updateCampaign(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCampaignRequest request) {
        return ApiResponse.ok("Campaign updated", flashSaleService.updateCampaign(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<Void> cancelCampaign(@PathVariable UUID id) {
        flashSaleService.cancelCampaign(id);
        return ApiResponse.ok("Campaign cancelled", null);
    }

    @PostMapping("/{id}/purchase")
    public ApiResponse<PurchaseResponse> purchase(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody PurchaseRequest request) {
        return ApiResponse.ok(flashSaleService.purchase(id, userId, userEmail, request));
    }
}
