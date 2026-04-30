package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponse {

    private UUID id;
    private UUID productId;
    private String sku;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }
}
