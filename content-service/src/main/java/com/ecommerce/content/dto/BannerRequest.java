package com.ecommerce.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class BannerRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 500)
    private String imageUrl;

    @Size(max = 500)
    private String linkUrl;

    private Integer displayOrder;
    private Boolean isActive;
    private Instant startDate;
    private Instant endDate;
}
