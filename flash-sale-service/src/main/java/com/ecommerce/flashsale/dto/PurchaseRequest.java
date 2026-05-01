package com.ecommerce.flashsale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseRequest {

    @NotNull
    private PaymentMethod paymentMethod;

    @NotBlank
    private String shippingName;

    @NotBlank
    private String shippingPhone;

    @NotBlank
    private String shippingAddress;
}
