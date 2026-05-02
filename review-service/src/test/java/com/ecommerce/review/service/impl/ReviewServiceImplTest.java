package com.ecommerce.review.service.impl;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.review.client.OrderServiceClient;
import com.ecommerce.review.dto.ReviewRequest;
import com.ecommerce.review.entity.Review;
import com.ecommerce.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    private ReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository, orderServiceClient);
    }

    @Test
    void confirmedPurchaseAllowsSubmit() {
        ReviewRequest request = request();
        when(orderServiceClient.hasConfirmedOrder("user-1", request.getProductId()))
                .thenReturn(ApiResponse.ok(Map.of("confirmed", true)));
        when(reviewRepository.existsByUserIdAndProductId("user-1", request.getProductId())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.submitReview("user-1", request);

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void unconfirmedPurchaseRejectsSubmit() {
        ReviewRequest request = request();
        when(orderServiceClient.hasConfirmedOrder("user-1", request.getProductId()))
                .thenReturn(ApiResponse.ok(Map.of("confirmed", false)));

        assertThatThrownBy(() -> service.submitReview("user-1", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You must purchase this product before reviewing");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void duplicateReviewRejectsSubmit() {
        ReviewRequest request = request();
        when(orderServiceClient.hasConfirmedOrder("user-1", request.getProductId()))
                .thenReturn(ApiResponse.ok(Map.of("confirmed", true)));
        when(reviewRepository.existsByUserIdAndProductId("user-1", request.getProductId())).thenReturn(true);

        assertThatThrownBy(() -> service.submitReview("user-1", request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("You have already reviewed this product");
    }

    @Test
    void nonOwnerCannotUpdate() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(Review.builder()
                .id(reviewId)
                .userId("owner")
                .productId(UUID.randomUUID())
                .rating(5)
                .build()));

        assertThatThrownBy(() -> service.updateReview(reviewId, "other-user", request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Unauthorized");
    }

    @Test
    void adminCanDeleteOthersReview() {
        UUID reviewId = UUID.randomUUID();
        Review review = Review.builder()
                .id(reviewId)
                .userId("owner")
                .productId(UUID.randomUUID())
                .rating(5)
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        service.deleteReview(reviewId, "admin-user", new UsernamePasswordAuthenticationToken(
                "admin-user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        verify(reviewRepository).delete(review);
    }

    private ReviewRequest request() {
        ReviewRequest request = new ReviewRequest();
        request.setProductId(UUID.randomUUID());
        request.setRating(5);
        request.setComment("Good");
        return request;
    }
}
