package com.ecommerce.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingResponse {

    private UUID productId;
    private Double averageRating;
    private Long reviewCount;
}
