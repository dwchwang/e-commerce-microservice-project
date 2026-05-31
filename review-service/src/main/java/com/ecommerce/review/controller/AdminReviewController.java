package com.ecommerce.review.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.review.dto.ReviewResponse;
import com.ecommerce.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminReviewController {

    private final ReviewRepository reviewRepository;

    @GetMapping
    public ApiResponse<List<ReviewResponse>> listReviews(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "100") int size) {
        return ApiResponse.ok(reviewRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(review -> userId == null || userId.isBlank() || userId.equals(review.getUserId()))
                .filter(review -> productId == null || productId.equals(review.getProductId()))
                .limit(Math.max(1, Math.min(size, 500)))
                .map(ReviewResponse::from)
                .toList());
    }
}
