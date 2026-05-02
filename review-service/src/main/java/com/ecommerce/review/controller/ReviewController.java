package com.ecommerce.review.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.review.dto.ProductRatingResponse;
import com.ecommerce.review.dto.ReviewRequest;
import com.ecommerce.review.dto.ReviewResponse;
import com.ecommerce.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ApiResponse<ReviewResponse> submitReview(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok("Review submitted successfully", reviewService.submitReview(userId, request));
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<Page<ReviewResponse>> getProductReviews(@PathVariable UUID productId, Pageable pageable) {
        return ApiResponse.ok(reviewService.getProductReviews(productId, pageable));
    }

    @GetMapping("/product/{productId}/rating")
    public ApiResponse<ProductRatingResponse> getProductRating(@PathVariable UUID productId) {
        return ApiResponse.ok(reviewService.getProductRating(productId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.updateReview(id, userId, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ApiResponse<Void> deleteReview(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            Authentication authentication) {
        reviewService.deleteReview(id, userId, authentication);
        return ApiResponse.ok("Review deleted successfully", null);
    }
}
