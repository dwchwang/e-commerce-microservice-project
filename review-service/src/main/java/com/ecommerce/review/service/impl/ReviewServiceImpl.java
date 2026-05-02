package com.ecommerce.review.service.impl;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.review.client.OrderServiceClient;
import com.ecommerce.review.dto.ProductRatingResponse;
import com.ecommerce.review.dto.ReviewRequest;
import com.ecommerce.review.dto.ReviewResponse;
import com.ecommerce.review.entity.Review;
import com.ecommerce.review.repository.ReviewRepository;
import com.ecommerce.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderServiceClient orderServiceClient;

    @Override
    @Transactional
    public ReviewResponse submitReview(String userId, ReviewRequest request) {
        requireUser(userId);
        ApiResponse<Map<String, Boolean>> check =
                orderServiceClient.hasConfirmedOrder(userId, request.getProductId());

        boolean confirmed = check != null
                && check.isSuccess()
                && check.getData() != null
                && Boolean.TRUE.equals(check.getData().get("confirmed"));

        if (!confirmed) {
            throw new BusinessException("You must purchase this product before reviewing");
        }
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new BusinessException("You have already reviewed this product");
        }

        Review review = Review.builder()
                .userId(userId)
                .productId(request.getProductId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable).map(ReviewResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingResponse getProductRating(UUID productId) {
        return reviewRepository.getAverageRating(productId)
                .orElseGet(() -> new ProductRatingResponse(productId, 0.0, 0L));
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID id, String userId, ReviewRequest request) {
        requireUser(userId);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("Unauthorized");
        }
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(UUID id, String userId, Authentication authentication) {
        requireUser(userId);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        if (!review.getUserId().equals(userId) && !isAdmin(authentication)) {
            throw new BusinessException("Unauthorized");
        }
        reviewRepository.delete(review);
    }

    private void requireUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("Unauthorized");
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
