package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.entity.UserProfile;
import com.ecommerce.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    public ApiResponse<Page<UserProfileResponse>> listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String normalizedQ = q == null ? "" : q.trim().toLowerCase();
        List<UserProfileResponse> filtered = userProfileRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(user -> normalizedQ.isBlank()
                        || safeLower(user.getEmail()).contains(normalizedQ)
                        || safeLower(user.getFullName()).contains(normalizedQ)
                        || safeLower(user.getPhoneNumber()).contains(normalizedQ))
                .map(this::toResponse)
                .toList();

        int pageSize = Math.max(1, Math.min(size, 200));
        int pageIndex = Math.max(0, page);
        int from = Math.min(pageIndex * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        Page<UserProfileResponse> result = new PageImpl<>(
                filtered.subList(from, to), PageRequest.of(pageIndex, pageSize), filtered.size());
        return ApiResponse.ok(result);
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
