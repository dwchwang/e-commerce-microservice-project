package com.ecommerce.identity.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.identity.dto.AssignRoleRequest;
import com.ecommerce.identity.dto.LoginRequest;
import com.ecommerce.identity.dto.RefreshTokenRequest;
import com.ecommerce.identity.dto.RegisterRequest;
import com.ecommerce.identity.dto.TokenResponse;
import com.ecommerce.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterRequest request) {
        String userId = authService.register(request);
        return ApiResponse.ok("User registered successfully", userId);
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.ok("Logged out successfully", null);
    }

    @PostMapping("/assign-role")
    public ApiResponse<Void> assignRole(
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String roles,
            @Valid @RequestBody AssignRoleRequest request) {
        if (!roles.contains("ROLE_ADMIN")) {
            throw new BusinessException("Admin role is required");
        }
        authService.assignRole(request);
        return ApiResponse.ok("Role assigned successfully", null);
    }
}
