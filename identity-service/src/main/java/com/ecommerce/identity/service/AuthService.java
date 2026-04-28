package com.ecommerce.identity.service;

import com.ecommerce.identity.dto.AssignRoleRequest;
import com.ecommerce.identity.dto.LoginRequest;
import com.ecommerce.identity.dto.RefreshTokenRequest;
import com.ecommerce.identity.dto.RegisterRequest;
import com.ecommerce.identity.dto.TokenResponse;

public interface AuthService {

    String register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void assignRole(AssignRoleRequest request);
}
