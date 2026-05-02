package com.ecommerce.content.repository;

import com.ecommerce.content.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

    @Query("""
           SELECT b
           FROM Banner b
           WHERE b.isActive = true
             AND (b.startDate IS NULL OR b.startDate <= :now)
             AND (b.endDate IS NULL OR b.endDate >= :now)
           ORDER BY b.displayOrder ASC, b.createdAt DESC
           """)
    List<Banner> findActiveBanners(@Param("now") Instant now);
}
