package com.ecommerce.user.service;

import com.ecommerce.user.dto.AddressRequest;
import com.ecommerce.user.dto.AddressResponse;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserProfileResponse getProfileByKeycloakId(String keycloakUserId);

    UserProfileResponse getProfileById(UUID userId);

    UserProfileResponse updateProfile(String keycloakUserId, UpdateProfileRequest request);

    List<AddressResponse> getAddresses(String keycloakUserId);

    AddressResponse addAddress(String keycloakUserId, AddressRequest request);

    AddressResponse updateAddress(String keycloakUserId, UUID addressId, AddressRequest request);

    void deleteAddress(String keycloakUserId, UUID addressId);
}
