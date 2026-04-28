package com.ecommerce.user.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 255)
    private String fullName;

    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 500)
    private String avatarUrl;
}
