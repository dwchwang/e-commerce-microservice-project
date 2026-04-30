package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.BusinessException;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/api/cart")
    public ApiResponse<CartResponse> getCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        return ApiResponse.ok(cartService.getCart(resolveCartKey(userId, sessionId)));
    }

    @PostMapping("/api/cart/items")
    public ApiResponse<CartResponse> addItem(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody AddToCartRequest request) {
        return ApiResponse.ok("Item added to cart", cartService.addItem(resolveCartKey(userId, sessionId), request));
    }

    @PutMapping("/api/cart/items/{productId}")
    public ApiResponse<CartResponse> updateItem(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.ok(cartService.updateItem(resolveCartKey(userId, sessionId), productId, request));
    }

    @DeleteMapping("/api/cart/items/{productId}")
    public ApiResponse<CartResponse> removeItem(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @PathVariable UUID productId) {
        return ApiResponse.ok(cartService.removeItem(resolveCartKey(userId, sessionId), productId));
    }

    @DeleteMapping("/api/cart")
    public ApiResponse<Void> clearCurrentCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        cartService.clearCart(resolveCartKey(userId, sessionId));
        return ApiResponse.ok("Cart cleared", null);
    }

    @DeleteMapping("/internal/cart/{userId}")
    public ApiResponse<Void> clearUserCart(@PathVariable String userId) {
        cartService.clearCart("cart:" + userId);
        return ApiResponse.ok("Cart cleared", null);
    }

    private String resolveCartKey(String userId, String sessionId) {
        if (userId != null && !userId.isBlank()) {
            return "cart:" + userId;
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return "cart:guest:" + sessionId;
        }
        throw new BusinessException("Either X-User-Id or X-Session-Id header is required");
    }
}
