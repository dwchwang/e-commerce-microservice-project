package com.ecommerce.order.dto;

import com.ecommerce.order.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<OrderItemRequest> items;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String voucherCode;

    @NotBlank(message = "Shipping name is required")
    private String shippingName;

    @NotBlank(message = "Shipping phone is required")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Invalid phone number")
    private String shippingPhone;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
}
