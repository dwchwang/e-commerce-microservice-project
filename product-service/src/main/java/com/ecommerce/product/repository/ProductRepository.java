package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndIsActiveTrue(UUID id);

    Optional<Product> findBySkuAndIsActiveTrue(String sku);

    boolean existsBySku(String sku);

    @Query(value = """
            SELECT * FROM products WHERE is_active = true
            AND (
                :keyword IS NULL
                OR to_tsvector('simple', name || ' ' || COALESCE(description, '') || ' ' || sku)
                   @@ plainto_tsquery('simple', :keyword)
                OR LOWER(name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:categoryId IS NULL OR category_id = CAST(:categoryId AS UUID))
            AND (:brandId IS NULL OR brand_id = CAST(:brandId AS UUID))
            AND (:minPrice IS NULL OR price >= CAST(:minPrice AS DECIMAL))
            AND (:maxPrice IS NULL OR price <= CAST(:maxPrice AS DECIMAL))
            """,
            countQuery = """
            SELECT COUNT(*) FROM products WHERE is_active = true
            AND (
                :keyword IS NULL
                OR to_tsvector('simple', name || ' ' || COALESCE(description, '') || ' ' || sku)
                   @@ plainto_tsquery('simple', :keyword)
                OR LOWER(name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:categoryId IS NULL OR category_id = CAST(:categoryId AS UUID))
            AND (:brandId IS NULL OR brand_id = CAST(:brandId AS UUID))
            AND (:minPrice IS NULL OR price >= CAST(:minPrice AS DECIMAL))
            AND (:maxPrice IS NULL OR price <= CAST(:maxPrice AS DECIMAL))
            """,
            nativeQuery = true)
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") String categoryId,
            @Param("brandId") String brandId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
