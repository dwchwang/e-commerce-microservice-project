package com.ecommerce.content.dto;

import com.ecommerce.content.entity.BlogPost;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BlogPostResponse {

    private UUID id;
    private String title;
    private String slug;
    private String content;
    private String thumbnailUrl;
    private String author;
    private Boolean isPublished;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static BlogPostResponse from(BlogPost post) {
        return BlogPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .thumbnailUrl(post.getThumbnailUrl())
                .author(post.getAuthor())
                .isPublished(post.getIsPublished())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
