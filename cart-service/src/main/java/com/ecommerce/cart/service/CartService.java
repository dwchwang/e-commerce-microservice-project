package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;

import java.util.UUID;

public interface CartService {

    CartResponse getCart(String cartKey);

    CartResponse addItem(String cartKey, AddToCartRequest request);

    CartResponse updateItem(String cartKey, UUID productId, UpdateCartItemRequest request);

    CartResponse removeItem(String cartKey, UUID productId);

    void clearCart(String cartKey);
}
