package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInRequest {

    @NotBlank
    @Size(max = 50)
    private String sku;

    @Size(max = 255)
    private String productName;

    @NotNull
    @Min(1)
    private Integer quantity;

    @Size(max = 500)
    private String note;
}
