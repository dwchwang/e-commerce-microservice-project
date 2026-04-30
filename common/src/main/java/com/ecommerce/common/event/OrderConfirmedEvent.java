package com.ecommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent implements Serializable {

    private UUID eventId;
    private UUID orderId;
    private String userId;
    private List<OrderItemEvent> items;
}
