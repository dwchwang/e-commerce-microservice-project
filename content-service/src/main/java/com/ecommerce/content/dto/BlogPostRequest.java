package com.ecommerce.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogPostRequest {

    @NotBlank
    @Size(max = 500)
    private String title;

    @Size(max = 500)
    private String slug;

    @NotBlank
    private String content;

    @Size(max = 500)
    private String thumbnailUrl;

    @Size(max = 255)
    private String author;

    private Boolean isPublished;
}
