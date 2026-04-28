package com.ecommerce.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private Boolean isActive;
    private List<String> imageUrls;
    private List<SpecificationDto> specs;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
