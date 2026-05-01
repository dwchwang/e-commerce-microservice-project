package com.ecommerce.flashsale.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.flashsale.dto.CampaignResponse;
import com.ecommerce.flashsale.dto.CreateCampaignRequest;
import com.ecommerce.flashsale.dto.PurchaseRequest;
import com.ecommerce.flashsale.dto.PurchaseResponse;
import com.ecommerce.flashsale.service.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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

    @GetMapping("/{id}")
    public ApiResponse<CampaignResponse> getCampaign(@PathVariable UUID id) {
        return ApiResponse.ok(flashSaleService.getCampaign(id));
    }

    @PostMapping
    public ApiResponse<CampaignResponse> createCampaign(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @Valid @RequestBody CreateCampaignRequest request) {
        requireAdmin(roles);
        return ApiResponse.ok("Campaign created", flashSaleService.createCampaign(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CampaignResponse> updateCampaign(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @Valid @RequestBody CreateCampaignRequest request) {
        requireAdmin(roles);
        return ApiResponse.ok("Campaign updated", flashSaleService.updateCampaign(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancelCampaign(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        requireAdmin(roles);
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

    private void requireAdmin(String roles) {
        if (roles == null || roles.isBlank() || !roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Admin role is required");
        }
    }
}
