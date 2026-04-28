package com.ecommerce.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String keycloakUserId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String avatarUrl;
    private Integer loyaltyPoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
