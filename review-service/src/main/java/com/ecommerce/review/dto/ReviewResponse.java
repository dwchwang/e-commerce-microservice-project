package com.ecommerce.review.dto;

import com.ecommerce.review.entity.Review;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {

    private UUID id;
    private String userId;
    private UUID productId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
    private Instant updatedAt;

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .productId(review.getProductId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
