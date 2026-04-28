package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    private UUID id;
    private String sku;
    private String name;
    private BigDecimal price;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private String primaryImageUrl;
}
