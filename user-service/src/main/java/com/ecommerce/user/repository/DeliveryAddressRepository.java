package com.ecommerce.user.repository;

import com.ecommerce.user.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, UUID> {

    List<DeliveryAddress> findByUserProfileIdOrderByCreatedAtDesc(UUID userId);

    Optional<DeliveryAddress> findByIdAndUserProfileId(UUID id, UUID userId);

    @Modifying
    @Query("update DeliveryAddress a set a.defaultAddress = false where a.userProfile.id = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
