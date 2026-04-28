package com.ecommerce.user.service.impl;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.user.dto.AddressRequest;
import com.ecommerce.user.dto.AddressResponse;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.entity.DeliveryAddress;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.DeliveryAddressRepository;
import com.ecommerce.user.repository.UserProfileRepository;
import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByKeycloakId(String keycloakUserId) {
        return toProfileResponse(findByKeycloakUserId(keycloakUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(UUID userId) {
        return toProfileResponse(userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "id", userId)));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String keycloakUserId, UpdateProfileRequest request) {
        UserProfile profile = findByKeycloakUserId(keycloakUserId);
        profile.setFullName(request.getFullName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setAvatarUrl(request.getAvatarUrl());
        return toProfileResponse(userProfileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(String keycloakUserId) {
        UserProfile profile = findByKeycloakUserId(keycloakUserId);
        return deliveryAddressRepository.findByUserProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(String keycloakUserId, AddressRequest request) {
        UserProfile profile = findByKeycloakUserId(keycloakUserId);
        if (Boolean.TRUE.equals(request.getDefaultAddress())) {
            deliveryAddressRepository.clearDefaultForUser(profile.getId());
        }
        DeliveryAddress address = DeliveryAddress.builder()
                .userProfile(profile)
                .recipientName(request.getRecipientName())
                .phoneNumber(request.getPhoneNumber())
                .addressLine(request.getAddressLine())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .defaultAddress(Boolean.TRUE.equals(request.getDefaultAddress()))
                .build();
        return toAddressResponse(deliveryAddressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String keycloakUserId, UUID addressId, AddressRequest request) {
        UserProfile profile = findByKeycloakUserId(keycloakUserId);
        DeliveryAddress address = deliveryAddressRepository.findByIdAndUserProfileId(addressId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAddress", "id", addressId));
        if (Boolean.TRUE.equals(request.getDefaultAddress())) {
            deliveryAddressRepository.clearDefaultForUser(profile.getId());
        }
        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine(request.getAddressLine());
        address.setWard(request.getWard());
        address.setDistrict(request.getDistrict());
        address.setCity(request.getCity());
        address.setDefaultAddress(Boolean.TRUE.equals(request.getDefaultAddress()));
        return toAddressResponse(deliveryAddressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(String keycloakUserId, UUID addressId) {
        UserProfile profile = findByKeycloakUserId(keycloakUserId);
        DeliveryAddress address = deliveryAddressRepository.findByIdAndUserProfileId(addressId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DeliveryAddress", "id", addressId));
        deliveryAddressRepository.delete(address);
    }

    private UserProfile findByKeycloakUserId(String keycloakUserId) {
        return userProfileRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "keycloakUserId", keycloakUserId));
    }

    private UserProfileResponse toProfileResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .keycloakUserId(profile.getKeycloakUserId())
                .email(profile.getEmail())
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .avatarUrl(profile.getAvatarUrl())
                .loyaltyPoints(profile.getLoyaltyPoints())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private AddressResponse toAddressResponse(DeliveryAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine(address.getAddressLine())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .defaultAddress(address.getDefaultAddress())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
