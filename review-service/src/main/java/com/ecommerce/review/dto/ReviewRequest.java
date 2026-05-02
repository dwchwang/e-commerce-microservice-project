package com.ecommerce.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewRequest {

    @NotNull
    private UUID productId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @Size(max = 5000)
    private String comment;
}
