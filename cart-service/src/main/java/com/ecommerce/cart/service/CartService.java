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

  /**
   * Merge guest cart into user cart after login.
   * Guest items are moved to the user's cart; guest cart is cleared.
   */
  CartResponse mergeGuestToUser(String guestSessionId, String userId);
}
