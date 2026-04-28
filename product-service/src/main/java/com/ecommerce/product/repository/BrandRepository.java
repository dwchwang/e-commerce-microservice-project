package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    boolean existsByName(String name);
}
