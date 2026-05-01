package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleOrderRequestedEvent implements Serializable {

    private UUID eventId;
    private String userId;
    private String userEmail;
    private UUID productId;
    private String sku;
    private String productName;
    private BigDecimal salePrice;
    private UUID flashSaleId;
    private String paymentMethod;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
}
