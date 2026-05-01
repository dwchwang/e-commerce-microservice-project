package com.ecommerce.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VnPayCreateResponse {

    private UUID paymentId;
    private UUID orderId;
    private String paymentUrl;
    private LocalDateTime expiresAt;
}
