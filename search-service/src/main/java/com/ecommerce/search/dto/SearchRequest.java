package com.ecommerce.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    private String q;
    private String categoryId;
    private String brandId;
    private Double minPrice;
    private Double maxPrice;

    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "relevance";
}
