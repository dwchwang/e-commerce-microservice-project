package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final UserProfileRepository userProfileRepository;

    @GetMapping
    public ApiResponse<List<UserProfileResponse>> listUsers(@RequestParam(required = false) String q) {
        String normalizedQ = q == null ? "" : q.trim().toLowerCase();
        return ApiResponse.ok(userProfileRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(user -> normalizedQ.isBlank()
                        || safeLower(user.getEmail()).contains(normalizedQ)
                        || safeLower(user.getFullName()).contains(normalizedQ)
                        || safeLower(user.getPhoneNumber()).contains(normalizedQ))
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.ok(userProfileRepository.findById(id)
                .map(this::toResponse)
                .orElse(null));
    }

    private UserProfileResponse toResponse(UserProfile user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .keycloakUserId(user.getKeycloakUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .loyaltyPoints(user.getLoyaltyPoints())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
