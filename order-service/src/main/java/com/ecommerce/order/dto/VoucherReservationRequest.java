package com.ecommerce.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherReservationRequest {

    @NotBlank
    private String userId;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal orderTotal;
}
