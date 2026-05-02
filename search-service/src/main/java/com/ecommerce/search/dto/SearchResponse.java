package com.ecommerce.search.dto;

import com.ecommerce.search.document.ProductDocument;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {

    private String id;
    private String sku;
    private String name;
    private String description;
    private Double price;
    private String categoryId;
    private String categoryName;
    private String brandId;
    private String brandName;
    private List<String> imageUrls;

    public static SearchResponse from(ProductDocument document) {
        return SearchResponse.builder()
                .id(document.getId())
                .sku(document.getSku())
                .name(document.getName())
                .description(document.getDescription())
                .price(document.getPrice())
                .categoryId(document.getCategoryId())
                .categoryName(document.getCategoryName())
                .brandId(document.getBrandId())
                .brandName(document.getBrandName())
                .imageUrls(document.getImageUrls())
                .build();
    }
}
