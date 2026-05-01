package com.ecommerce.flashsale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {

    private boolean success;
    private String message;
    private UUID campaignId;
    private Integer remainingStock;

    public static PurchaseResponse success(UUID campaignId, Integer remainingStock) {
        return PurchaseResponse.builder()
                .success(true)
                .message("Flash sale slot reserved; order creation is processing")
                .campaignId(campaignId)
                .remainingStock(remainingStock)
                .build();
    }
}
