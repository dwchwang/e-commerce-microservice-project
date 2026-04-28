package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.dto.AddressRequest;
import com.ecommerce.user.dto.AddressResponse;
import com.ecommerce.user.dto.UpdateProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(userService.getProfileByKeycloakId(userId));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(userId, request));
    }

    @GetMapping("/me/addresses")
    public ApiResponse<List<AddressResponse>> getMyAddresses(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(userService.getAddresses(userId));
    }

    @PostMapping("/me/addresses")
    public ApiResponse<AddressResponse> addAddress(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok("Address created successfully", userService.addAddress(userId, request));
    }

    @PutMapping("/me/addresses/{addressId}")
    public ApiResponse<AddressResponse> updateAddress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        return ApiResponse.ok(userService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID addressId) {
        userService.deleteAddress(userId, addressId);
        return ApiResponse.ok("Address deleted successfully", null);
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileResponse> getProfileById(@PathVariable UUID userId) {
        return ApiResponse.ok(userService.getProfileById(userId));
    }
}
