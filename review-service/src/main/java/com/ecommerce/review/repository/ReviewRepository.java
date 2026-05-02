package com.ecommerce.review.repository;

import com.ecommerce.review.dto.ProductRatingResponse;
import com.ecommerce.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByUserIdAndProductId(String userId, UUID productId);

    Page<Review> findByProductId(UUID productId, Pageable pageable);

    @Query("""
           SELECT new com.ecommerce.review.dto.ProductRatingResponse(
               r.productId,
               COALESCE(AVG(r.rating), 0),
               COUNT(r)
           )
           FROM Review r
           WHERE r.productId = :productId
           GROUP BY r.productId
           """)
    Optional<ProductRatingResponse> getAverageRating(@Param("productId") UUID productId);
}
