package com.ecommerce.flashsale.service;

import com.ecommerce.flashsale.dto.CampaignResponse;
import com.ecommerce.flashsale.dto.CreateCampaignRequest;
import com.ecommerce.flashsale.dto.PurchaseRequest;
import com.ecommerce.flashsale.dto.PurchaseResponse;

import java.util.List;
import java.util.UUID;

public interface FlashSaleService {

    List<CampaignResponse> getActiveCampaigns();

    List<CampaignResponse> getAllCampaigns();

    CampaignResponse getCampaign(UUID campaignId);

    CampaignResponse createCampaign(CreateCampaignRequest request);

    CampaignResponse updateCampaign(UUID campaignId, CreateCampaignRequest request);

    void cancelCampaign(UUID campaignId);

    PurchaseResponse purchase(UUID campaignId, String userId, String userEmail, PurchaseRequest request);
}
