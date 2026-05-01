package com.ecommerce.flashsale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateCampaignRequest {

    @NotNull
    private UUID productId;

    @NotBlank
    private String sku;

    @NotBlank
    private String productName;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal originalPrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal salePrice;

    @NotNull
    @Positive
    private Integer quantity;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    @Future
    private LocalDateTime endTime;
}
