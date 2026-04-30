package com.ecommerce.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductResponse {

    private UUID id;
    private String sku;
    private String name;
    private BigDecimal price;
    private Boolean isActive;
}
