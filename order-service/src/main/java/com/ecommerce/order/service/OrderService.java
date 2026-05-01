package com.ecommerce.order.service;

import com.ecommerce.common.event.FlashSaleOrderRequestedEvent;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PlaceOrderRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse placeOrder(String userId, String userEmail, PlaceOrderRequest request);

    OrderResponse createFlashSaleOrder(FlashSaleOrderRequestedEvent event);

    List<OrderResponse> getUserOrders(String userId);

    OrderResponse getUserOrder(String userId, UUID orderId);

    String getOrderStatus(String userId, UUID orderId);

    boolean hasConfirmedOrderForProduct(String userId, UUID productId);
}
