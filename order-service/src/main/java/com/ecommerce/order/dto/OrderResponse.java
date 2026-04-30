package com.ecommerce.order.dto;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {

    private UUID id;
    private String userId;
    private String userEmail;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String voucherCode;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String cancelReason;
    private Boolean isFlashSale;
    private UUID flashSaleId;
    private LocalDateTime reservationExpiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userEmail(order.getUserEmail())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .voucherCode(order.getVoucherCode())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getShippingAddress())
                .cancelReason(order.getCancelReason())
                .isFlashSale(order.getIsFlashSale())
                .flashSaleId(order.getFlashSaleId())
                .reservationExpiredAt(order.getReservationExpiredAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems().stream()
                        .sorted(Comparator.comparing(OrderItem::getSku))
                        .map(OrderItemResponse::from)
                        .toList())
                .build();
    }
}
