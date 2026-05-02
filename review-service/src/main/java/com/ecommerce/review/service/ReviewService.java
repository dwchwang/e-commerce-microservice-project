package com.ecommerce.review.service;

import com.ecommerce.review.dto.ProductRatingResponse;
import com.ecommerce.review.dto.ReviewRequest;
import com.ecommerce.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse submitReview(String userId, ReviewRequest request);

    Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable);

    ProductRatingResponse getProductRating(UUID productId);

    ReviewResponse updateReview(UUID id, String userId, ReviewRequest request);

    void deleteReview(UUID id, String userId, Authentication authentication);
}
